package edu.eckerd.integrations.slate.emergencycontact

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import concurrent.duration.SECONDS
import concurrent.duration.Duration
import concurrent.{Await, ExecutionContext, Future}
import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.methods.{Courier, EmergencyContactMethods, SlateToData, jsonParserProtocol}
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.{DBFunctions, SPREMRG}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.annotation.tailrec
import concurrent.ExecutionContext.Implicits.global

/**
  * Created by davenpcm on 6/29/16.
  */
object MainApplication extends SlateToData with jsonParserProtocol with EmergencyContactMethods with LazyLogging with App {

  val request = GetRequestConfiguration()
  implicit val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("oracle")
  implicit val profile = dbConfig.driver
  implicit val db = dbConfig.db
  implicit val system = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  logger.info("Starting Slate Emergency Request Transfer")

  val dataF = TransformData[SlateEmergencyContactInfo](request.link, request.user, request.password)

  Await.result( ProcessRequests(dataF), Duration.Inf)

  val terminate = system.terminate()
  Await.result(system.whenTerminated, Duration(10, SECONDS))

  logger.info("Exiting Slate Emergency Request Transfer Normally")
}
