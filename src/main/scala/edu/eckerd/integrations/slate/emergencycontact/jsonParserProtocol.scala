package edu.eckerd.integrations.slate.emergencycontact

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import edu.eckerd.integrations.slate.emergencycontact.model.SlateResponse
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import spray.json._
import spray.json.DefaultJsonProtocol

/**
  * Created by davenpcm on 6/29/16.
  */
trait jsonParserProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def EmergencyContactFormat = jsonFormat8(SlateEmergencyContactInfo)
  implicit def SlateResponseFormat[A : JsonFormat] = jsonFormat1(SlateResponse.apply[A])
}
