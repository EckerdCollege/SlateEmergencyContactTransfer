package edu.eckerd.integrations.slate.emergencycontact
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import concurrent.duration.SECONDS
import concurrent.duration.Duration
import concurrent.{Await, Future}
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.model.{SlateEmergencyContactInfo, SlateResponse}
import edu.eckerd.integrations.slate.emergencycontact.persistence.{DBFunctions, SPREMRG}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.annotation.tailrec
//import spray.json._

//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshallerConverter
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonUnmarshaller
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonUnmarshallerConverter
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
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

  val printF = for {
    data <- dataF
  } yield for {
    response <- data
  } yield {
    response
  }

  val data = Await.result(printF, Duration.Inf)

//  val prioritiesFixed = data.groupBy(_.BannerID).map(_._2.toList).flatMap(changePriorities(_))

  alterDataToCorrectPriorites(data).map(_.BannerID).map(t => (t, Await.result(getPidmFromBannerID(t), Duration.Inf))).foreach(println)



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


  def parsePhone(contactInfo: SlateEmergencyContactInfo): PhoneNumber = {
    val parse = contactInfo.ECCell.getOrElse("").replace("+", "").replace(".", "-").replace(" ", "-")
    parse match {
      case usNumber if usNumber.startsWith("1-") && usNumber.length == 14 =>
        val areaCode = usNumber.dropWhile(_ != '-').drop(1).takeWhile(_ != '-')
        val phoneNumber = usNumber.dropWhile(_ != '-').drop(1).dropWhile(_ != '-').drop(1).replace("-", "")
        UsPhoneNumber("1", areaCode, phoneNumber )
      case anythingElse =>
        val textBlob = anythingElse.replace("-", "")
        val fakeAreaCodeOfFirstThreeNumbersDontBlameMePlease = textBlob.take(3)
        val restOfNumberThatIsCompleteConstructDontBlameMePlase = textBlob.drop(3)
        IntlPhoneNumber(
          fakeAreaCodeOfFirstThreeNumbersDontBlameMePlease,
          restOfNumberThatIsCompleteConstructDontBlameMePlase
        )
    }
  }

  sealed trait PhoneNumber {
    val areaCode: String
    val phoneNumber: String
  }

  case class UsPhoneNumber(
                          natnCode: String,
                          areaCode: String,
                          phoneNumber: String
                          ) extends PhoneNumber

  case class IntlPhoneNumber(
                              areaCode: String,
                              phoneNumber: String
                             ) extends PhoneNumber





//  val KillActorSystem = for {
//    printed <- printF
//    terminate <- system.terminate()
//  } yield terminate

  Await.result(system.terminate(), Duration.Inf)
}
