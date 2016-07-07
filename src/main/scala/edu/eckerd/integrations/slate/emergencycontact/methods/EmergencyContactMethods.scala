package edu.eckerd.integrations.slate.emergencycontact.methods

import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.SpremrgRow
import edu.eckerd.integrations.slate.emergencycontact.persistence.DBFunctions
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by davenpcm on 7/7/16.
  */
trait EmergencyContactMethods extends DBFunctions{
  /**
    * This takes the list by bannerID and then changes The Priority List from wherever they happened to be in the form
    * to the appropriate number. This creates a List of Lists passes the individual lists to be parsed by
    * changePriorities and then returns a singular List as the call to changePriorities was flatMapped.
    *
    * @param seq The slate emergency contact information
    * @return An organized list of Emergency Contact Info
    */
  def alterDataToCorrectPriorites(seq: Seq[SlateEmergencyContactInfo]): List[SlateEmergencyContactInfo] = {
    seq
      .groupBy(_.BannerID)
      .map(_._2.toList)
      .flatMap(changePriorities)
      .toList
  }

  def changePriorities(listGroupedByBannerID: List[SlateEmergencyContactInfo]): List[SlateEmergencyContactInfo] = {

    @tailrec
    def internalChangePriorities(
                                  listGroupedByBannerID: List[SlateEmergencyContactInfo],
                                  recurse: Int,
                                  acc: List[SlateEmergencyContactInfo]
                                )
    : List[SlateEmergencyContactInfo] =listGroupedByBannerID match {
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

        internalChangePriorities(xs, recurse +1, newAcc)
    }

    internalChangePriorities(listGroupedByBannerID, 1, List[SlateEmergencyContactInfo]())
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
            (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[List[Either[SlateEmergencyContactInfo, SpremrgRow]]] = Future.sequence{
    for {
      record <- alterDataToCorrectPriorites(records)
    } yield for {
      pidm <- getPidmFromBannerID(record.BannerID)
      map <- generateRelationshipMap()
    } yield {

      val firstName = record.ECName.takeWhile(_ != ' ')
      val lastName = record.ECName.dropWhile(_ != ' ').drop(1)
      val relationshipCode = map.get(record.ECRelationship)

      val phone = parsePhone(record)
      val validZip = record.ECAddressPostal.map(_.length <= 30)

      val either = (pidm, phone, validZip) match {
        case (Some(pid), Right(usPhoneNumber), Some(true)) =>
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
        case (_ , _, _) =>
          Left(record)
      }

      either
    }
  }

  def ProcessRequests(allRequests: Future[Seq[SlateEmergencyContactInfo]])
                     (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[Unit] = {
    partitionToGroups(allRequests).flatMap{
      case (compliant, nonCompliant) =>
        dealWithCompliantRecords(compliant)
        dealWithNonCompliantRecords(nonCompliant)
    }

  }

  def partitionToGroups(allRequests: Future[Seq[SlateEmergencyContactInfo]])
                       (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[(List[SpremrgRow], List[SlateEmergencyContactInfo])] = {

    allRequests.flatMap(toRows).map(partitionFromEitherToGroups)
  }

  private def partitionFromEitherToGroups(list: List[Either[SlateEmergencyContactInfo, SpremrgRow]])
                                         (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : (List[SpremrgRow], List[SlateEmergencyContactInfo]) = {

    val partition = list.partition(_.isRight)
    val rowsForDB = partition._1.map(_.right.get)
    val rowsForManualEntry = partition._2.map(_.left.get)

    (rowsForDB, rowsForManualEntry)
  }


  private def dealWithNonCompliantRecords(list: List[SlateEmergencyContactInfo])
                                         (implicit ec: ExecutionContext): Future[Unit] = {
    val content = list.map(printEmergencyContactInfoAsHTMLRow).foldRight("")(_ + _)
    Courier.sendManualParseEmail(content)
  }

  private def dealWithCompliantRecords(list: List[SpremrgRow])
                                      (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[Unit] = {
    UpdateDB(list)
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

}
