package edu.eckerd.integrations.slate.emergencycontact

import concurrent.duration.SECONDS
import concurrent.duration.Duration
import concurrent.Await
import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.methods.{EmergencyContactMethods, SlateToData, jsonParserProtocol}
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.DBImpl

import concurrent.ExecutionContext.Implicits.global

/**
  * Created by davenpcm on 6/29/16.
  */
object MainApplication
  extends SlateToData
    with jsonParserProtocol
    with EmergencyContactMethods
    with DBImpl
    with LazyLogging
    with App {

  logger.info("Starting Slate Emergency Request Transfer")

  Await.result(
    ProcessRequests(TransformData[SlateEmergencyContactInfo](requestForConfig("slate"))) ,
    Duration(60, SECONDS)
  )

  logger.info("Exiting Slate Emergency Request Transfer Normally")
}
