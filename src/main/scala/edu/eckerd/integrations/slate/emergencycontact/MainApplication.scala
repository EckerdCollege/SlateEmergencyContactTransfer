package edu.eckerd.integrations.slate.emergencycontact

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import concurrent.duration.Duration
import concurrent.{Await, ExecutionContext, Future}
import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.{DBFunctions, SPREMRG}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.annotation.tailrec
import concurrent.ExecutionContext.Implicits.global

/**
  * Created by davenpcm on 6/29/16.
  */
object MainApplication extends SlateToData with jsonParserProtocol with DBFunctions with SPREMRG with LazyLogging with App {

  val request = GetRequestConfiguration()
  implicit val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("oracle")
  implicit val profile = dbConfig.driver
  implicit val db = dbConfig.db
  implicit val system = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  val dataF = TransformData[SlateEmergencyContactInfo](request.link, request.user, request.password)

  val rowsF = dataF.flatMap(toRows).map(PartitionToGroups)

  val sticklers = rowsF.map(_._2).map(DealWithNonCompliantRecords).flatMap(Courier.sendManualParseEmail)
  val writeToDB = rowsF.map(_._1).flatMap(UpdateDB)

  Await.result(sticklers, Duration.Inf)
  Await.result(writeToDB, Duration.Inf)




  def alterDataToCorrectPriorites(seq: Seq[SlateEmergencyContactInfo]): List[SlateEmergencyContactInfo] = {
    seq.groupBy(_.BannerID).map(_._2.toList).flatMap(changePriorities(_)).toList
  }


  @tailrec
  def changePriorities(listGroupedByBannerID: List[SlateEmergencyContactInfo],
                       recurse: Int = 1,
                       acc: List[SlateEmergencyContactInfo] = List[SlateEmergencyContactInfo]()
                      )
  : List[SlateEmergencyContactInfo] = listGroupedByBannerID match {
    case Nil =>
      acc.reverse
    case x :: xs =>
      val newAcc = SlateEmergencyContactInfo(
        x.BannerID,
        recurse.toString,
        x.ECName,
        x.ECRelationship,
        x.ECCell,
        x.ECAddressStreet,
        x.ECAddressCity,
        x.ECAddressPostal
      ) :: acc

    changePriorities(xs, recurse +1, newAcc)
  }


  def parsePhone(contactInfo: SlateEmergencyContactInfo): Either[String, PhoneNumber] = {
    val number = contactInfo.ECCell.getOrElse("")
    val parse = number.replace("+", "").replace(".", "-").replace(" ", "-")
    parse match {
      case usNumber if usNumber.startsWith("1-") && usNumber.length == 14 =>
        val areaCode = usNumber.dropWhile(_ != '-').drop(1).takeWhile(_ != '-')
        val phoneNumber = usNumber.dropWhile(_ != '-').drop(1).dropWhile(_ != '-').drop(1).replace("-", "")
        Right(PhoneNumber("1", Some(areaCode), phoneNumber ))
//      case intlParsed if intlParsed.dropWhile(_ != "-").drop(1).length <= 12 && !intlParsed.startsWith("1-") =>
//        val natnCode = intlParsed.takeWhile(_ != "-")
//        val phoneNumber = intlParsed.dropWhile(_ != "-").drop(1).replace("-", "")
//        Right(PhoneNumber(natnCode, None, phoneNumber ))
      case _ => Left(number)
    }
  }

  def toRows(records: Seq[SlateEmergencyContactInfo])
  : Future[List[Either[SlateEmergencyContactInfo, SpremrgRow]]] = Future.sequence{
    for {
      record <- alterDataToCorrectPriorites(records)
    } yield for {
      pidm <- getPidmFromBannerID(record.BannerID)
      map <- generateRelationshipMap()
    } yield {

      val firstName = record.ECName.takeWhile(_ != ' ')
      val lastName = record.ECName.dropWhile(_ != ' ').drop(1)
      val phone = parsePhone(record)

      val relationshipCode = map.get(record.ECRelationship)


      val either = (pidm, phone) match {
        case (Some(pid), Right(usPhoneNumber)) =>
          Right(
            SpremrgRow(
              pid,
              record.ECPriority.charAt(0),
              lastName,
              firstName,
              record.ECAddressStreet,
              record.ECAddressCity,
              record.ECAddressPostal,
              Some(usPhoneNumber.natnCode),
              usPhoneNumber.areaCode,
              Some(usPhoneNumber.phoneNumber),
              relationshipCode,
              new java.sql.Timestamp(new java.util.Date().getTime),
              Some("Slate Transfer"),
              Some("slate")
            )
          )
        case (_ , _) =>
          Left(record)
      }

      either
    }
  }

  def PartitionToGroups(list: List[Either[SlateEmergencyContactInfo, SpremrgRow]]):
  (List[SpremrgRow], List[SlateEmergencyContactInfo]) = {

    val partition = list.partition(_.isRight)
    val rowsForDB = partition._1.map(_.right.get)
    val rowsForManualEntry = partition._2.map(_.left.get)

    (rowsForDB, rowsForManualEntry)
  }

  /**
    * Update The Database And Then Throw It away
    * @param list List of All Users To Update
    * @param db Database to write To
    * @param ec Execution Context to Fork Processes Off Of
    * @return Unit. Fire and Forget On The Edge of The Application
    */
  def UpdateDB(list: List[SpremrgRow])(implicit db: JdbcProfile#Backend#Database, ec: ExecutionContext): Future[Unit] = {
    import profile.api._

    val r = for {
      result <- db.run(Spremrg ++= list)
    } yield result

    r.map(_ => ())
  }

  def DealWithNonCompliantRecords(list: List[SlateEmergencyContactInfo]): String = {
    list.map(printEmergencyContactInfoAsHTMLRow).foldRight("")(_ + _)
  }

  def prettyPrintEmergencyContactInfo(slateEmergencyContactInfo: SlateEmergencyContactInfo): String = {
      s"""BannerID         - ${slateEmergencyContactInfo.BannerID}
      |Priority Number  - ${slateEmergencyContactInfo.ECPriority}
      |Name             - ${slateEmergencyContactInfo.ECName}
      |Relationship     - ${slateEmergencyContactInfo.ECRelationship}
      |Phone Number     - ${slateEmergencyContactInfo.ECCell.getOrElse("")}
      |Street Address   - ${slateEmergencyContactInfo.ECAddressStreet.getOrElse("")}
      |City             - ${slateEmergencyContactInfo.ECAddressCity.getOrElse("")}
      |Zip Code         - ${slateEmergencyContactInfo.ECAddressPostal.getOrElse("")}""".stripMargin
  }

  def printEmergencyContactInfoAsHTMLRow(slateEmergencyContactInfo: SlateEmergencyContactInfo): String = {
    s"""
       |<tr>
       |  <td>${slateEmergencyContactInfo.BannerID}</td>
       |  <td>${slateEmergencyContactInfo.ECPriority}</td>
       |  <td>${slateEmergencyContactInfo.ECName}</td>
       |  <td>${slateEmergencyContactInfo.ECRelationship}</td>
       |  <td>${slateEmergencyContactInfo.ECCell.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressStreet.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressCity.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressPostal.getOrElse("")}</td>
       |</tr>
     """.stripMargin
  }

  case class PhoneNumber(
                          natnCode: String,
                          areaCode: Option[String],
                          phoneNumber: String
                          )

//  val emailSend = Courier.sendQuickEmail()
//
//  Await.result(emailSend, Duration.Inf)

  Await.result(system.terminate(), Duration.Inf)
}
