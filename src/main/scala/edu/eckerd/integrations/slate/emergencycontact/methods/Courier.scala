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

package edu.eckerd.integrations.slate.emergencycontact.methods

import com.typesafe.config.ConfigFactory
import edu.eckerd.integrations.slate.emergencycontact.methods.mail.Mail

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Courier {
  val config = ConfigFactory.load()
  val sender = config.getString("courier.sender.user")
  val senderPassword = config.getString("courier.sender.password")
  val senderDomain = config.getString("courier.sender.domain")
  val recipient = config.getString("courier.recipient.user")
  val recipientDomain = config.getString("courier.recipient.domain")

  /**
   * Creates an Email From a piece of content that is formulated as a 8 column table with headers in place for
   * Emergency Contact Parsing
   * @param content The content to be send in the email
   * @return Nothing You dont get anything when you send an email.
   */
  def sendManualParseEmail(content: String): Future[Unit] = {

    Future(
    Mail(
      from = sender + "@" + senderDomain -> sender,
      to = Seq(recipient + "@" + recipientDomain),
      subject = "Unparsed Emergency Contacts",
      message = "Attached are the Unparsed Emergency Contacts",
      richMessage = Some(
        """
          |<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
          |<html xmlns="http://www.w3.org/1999/xhtml">
          |    <head>
          |        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          |        <title></title>
          |        <style></style>
          |    </head>
          |    <body>
          |        <table border="0" cellpadding="0" cellspacing="0" height="100%" width="100%" id="bodyTable">
          |            <tr>
          |                <td align="center" valign="top">
          |                    <table border="0" cellpadding="0" cellspacing="0" width="800" id="emailContainer">
          |                        <tr>
          |                            <td align="center" valign="top">
          |                                <h2>Unparsed Emergency Contacts</h2>
          |                            </td>
          |                        </tr>
          |                        <table border="0" cellpadding="2" cellspacing="2" height="100%" width=100% id="emergencycontact">
          |                        <tr>
          |                        <th>Banner ID</th>
          |                        <th>Priority Number</th>
          |                        <th>Name</th>
          |                        <th>Relationship</th>
          |                        <th>Phone Number</th>
          |                        <th>Street Address</th>
          |                        <th>City</th>
          |                        <th>State</th>
          |                        <th>Zip Code</th>
          |                        </tr>""".stripMargin +
          content +
          """
            |                        </table>
            |                    </table>
            |                </td>
            |            </tr>
            |        </table>
            |    </body>
            |</html>
          """.stripMargin
      )

    ).send()
    )

  }

}

