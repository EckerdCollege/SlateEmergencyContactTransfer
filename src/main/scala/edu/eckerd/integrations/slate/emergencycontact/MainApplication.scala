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
object MainApplication extends SlateToData with jsonParserProtocol with LazyLogging with App {

  val request = GetRequestConfiguration()

  implicit val system = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  val dataF = TransformData[SlateEmergencyContactInfo](request.link, request.user, request.password)

  val printF = for {
    data <- dataF
  } yield for {
    response <- data
  } yield logger.debug(s"$response")

  val KillActorSystem = for {
    printed <- printF
    terminate <- system.terminate()
  } yield terminate

  Await.result(KillActorSystem, Duration.Inf)
}
