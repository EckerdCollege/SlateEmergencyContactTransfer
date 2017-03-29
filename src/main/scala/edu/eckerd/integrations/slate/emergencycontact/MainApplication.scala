/*
 * Copyright 2016 Eckerd College
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.eckerd.integrations.slate.emergencycontact

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }

import concurrent.duration.SECONDS
import concurrent.duration.Duration
import concurrent.Await
import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.methods.EmergencyContactMethods
import edu.eckerd.integrations.slate.emergencycontact.methods.EmergencyContactJsonProtocol
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.DBImpl
import edu.eckerd.integrations.slate.core.Request

import concurrent.ExecutionContext.Implicits.global

object MainApplication
    extends EmergencyContactMethods
    with DBImpl
    with LazyLogging
    with App {
  import EmergencyContactJsonProtocol._

  logger.info("Starting Slate Emergency Request Transfer")

  val actions = Request.SingleRequestForConfig[SlateEmergencyContactInfo]("slate")
    .flatMap(ProcessRequests)

  Await.result(
    actions,
    Duration(60, SECONDS)
  )

  logger.info("Exiting Slate Emergency Request Transfer Normally")
}
