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

import concurrent.ExecutionContext.Implicits.global

/**
  * Created by davenpcm on 6/29/16.
  */
object MainApplication extends App with LazyLogging{


  val config = ConfigFactory.load()
  val slateConfig = config.getConfig("slate")
  val user = slateConfig.getString("user")
  if (user == "") throw new Error("Slate Username is Blank")
  val password = slateConfig.getString("password")
  if (password == "") throw new Error("Slate Password is Blank")
  val link = slateConfig.getString("link")
  if (link == "") throw new Error("Slate Link is Blank")

  implicit val system = ActorSystem("EmergencyContact")
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  val authorization = Authorization(BasicHttpCredentials(user, password))

  val responseFuture = Http(system).singleRequest(HttpRequest(
    uri = link,
    headers = List(authorization)
  ))

  val response = Await.result(responseFuture, Duration.Inf)

  response match {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      val entityF = Unmarshal(entity).to[String]
      val entityHere = Await.result(entityF, Duration.Inf)
    logger.debug(entityHere.toString)

    case HttpResponse(code, _, _, _) =>
      val codeVal = code.value
      if (codeVal != "500 Internal Server Error"){
        logger.error("Invalid Status Code: " + codeVal)

      } else {
        logger.debug(s"Server Failure : $codeVal")
      }
  }
  Http().shutdownAllConnectionPools().onComplete{ _ =>
    system.terminate
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
