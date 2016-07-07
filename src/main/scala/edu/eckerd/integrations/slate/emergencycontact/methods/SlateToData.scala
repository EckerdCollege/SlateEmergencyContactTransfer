package edu.eckerd.integrations.slate.emergencycontact.methods

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import edu.eckerd.integrations.slate.emergencycontact.model.{SlateRequest, SlateResponse}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by davenpcm on 6/30/16.
  */
trait SlateToData {



  def GetRequestConfiguration() : SlateRequest = {
    val config = ConfigFactory.load()
    val slateConfig = config.getConfig("slate")
    val user = slateConfig.getString("user")
    if (user == "") throw new Error("Slate Username is Blank")
    val password = slateConfig.getString("password")
    if (password == "") throw new Error("Slate Password is Blank")
    val link = slateConfig.getString("link")
    if (link == "") throw new Error("Slate Link is Blank")
    SlateRequest(link, user, password)
  }

  def TransformData[A](link: String, user: String, password: String )
                      (implicit system: ActorSystem,
                       materializer: ActorMaterializer,
                       ec : ExecutionContext,
                      um: Unmarshaller[ResponseEntity, SlateResponse[A]]
                      ): Future[Seq[A]] = {
    val authorization = Authorization(BasicHttpCredentials(user, password))

    val responseFuture = Http(system).singleRequest(HttpRequest(
      uri = link,
      headers = List(authorization)
    ))



    responseFuture.flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Http().shutdownAllConnectionPools()
        val entityF = Unmarshal(entity).to[SlateResponse[A]]
        entityF.map(_.row)

      case HttpResponse(code, _, _, _) =>
        Http().shutdownAllConnectionPools()
        Future.failed(new Throwable(s"Received invalid response code - $code"))
    }
  }
}
