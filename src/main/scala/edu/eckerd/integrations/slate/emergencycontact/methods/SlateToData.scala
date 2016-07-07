package edu.eckerd.integrations.slate.emergencycontact.methods

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import edu.eckerd.integrations.slate.emergencycontact.model.{SlateRequest, SlateResponse}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by davenpcm on 6/30/16.
  */
trait SlateToData extends jsonParserProtocol {

  def requestForConfig(configLocation: String) : SlateRequest = {
    val config = ConfigFactory.load()
    val slateConfig = config.getConfig(configLocation)
    val user = slateConfig.getString("user")
    val password = slateConfig.getString("password")
    val link = slateConfig.getString("link")
    SlateRequest(link, user, password)
  }

  def TransformData[A](slateRequest: SlateRequest)
                      (
                        implicit ec : ExecutionContext,
                        um: Unmarshaller[ResponseEntity, SlateResponse[A]]
                      ): Future[Seq[A]] = {

    implicit val system = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    val authorization = Authorization(BasicHttpCredentials(slateRequest.user, slateRequest.password))

    val responseFuture = Http(system).singleRequest(HttpRequest(
      uri = slateRequest.link,
      headers = List(authorization)
    ))

    responseFuture.flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        for {
          slateResponse <- Unmarshal(entity).to[SlateResponse[A]]
          shutdownHttp <- Http(system).shutdownAllConnectionPools()
          terminate <- system.terminate()
        } yield slateResponse.row
      case HttpResponse(code, _, _, _) =>
        for {
          shutdownHttp <- Http(system).shutdownAllConnectionPools()
          terminate <- system.terminate()
          failure <- Future.failed(new Throwable(s"Received invalid response code - $code"))
        } yield failure
    }
  }
}
