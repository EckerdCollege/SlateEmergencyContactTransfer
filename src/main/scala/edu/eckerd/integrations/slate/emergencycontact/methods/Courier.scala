package edu.eckerd.integrations.slate.emergencycontact.methods

import javax.mail.internet.InternetAddress

import com.typesafe.config.ConfigFactory
import courier.Defaults._
import courier.{Envelope, Mailer, Multipart}

import scala.concurrent.Future
/**
  * Created by davenpcm on 7/6/16.
  */
object Courier {
  val config = ConfigFactory.load()
  val sender = config.getString("courier.sender.user")
  val senderPassword = config.getString("courier.sender.password")
  val senderDomain = config.getString("courier.sender.domain")
  val recipient = config.getString("courier.recipient.user")
  val recipientDomain = config.getString("courier.recipient.domain")


  /**
    * This is the implicit class which can convert automatically a string to this class which has these functions
    * available to it. Therefore taking  an normal string and where this class is in scope giving it additional
    * functionality. Such as being an internet address with the write calls.
    * @param name The first part of an email or the full address if the caller intends to call addr
    */
  implicit class addr(name: String){
    def `@`(domain: String): InternetAddress = new InternetAddress(s"$name@$domain")
    def at = `@` _
    /** In case whole string is email address already */
    def addr = new InternetAddress(name)
  }

  /**
    * This is the mailer used to send the email.
    */
  val mailer = Mailer("smtp.gmail.com", 587)
    .auth(true)
    .as(s"$sender@$senderDomain", senderPassword)
    .startTtls(true)()

  /**
    * Creates an Email From a piece of content that is formulated as a 8 column table with headers in place for
    * Emergency Contact Parsing
    * @param content The content to be send in the email
    * @return Nothing You dont get anything when you send an email.
    */
  def sendManualParseEmail(content: String): Future[Unit] = {
    mailer(
      Envelope
        .from(sender `@` senderDomain)
        .to(recipient `@` recipientDomain)
        .subject("Unparsed Emergency Contacts")
        .content( Multipart()
            .html(
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
        )
    )

  }

}


