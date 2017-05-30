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

import com.google.i18n.phonenumbers.PhoneNumberUtil
import edu.eckerd.integrations.slate.emergencycontact.model.SlateEmergencyContactInfo
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.SpremrgRow
import edu.eckerd.integrations.slate.emergencycontact.persistence.DBFunctions
import slick.jdbc.JdbcProfile

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Created by davenpcm on 7/7/16.
 */
trait EmergencyContactMethods extends DBFunctions {

  /**
   * This is the external API to the application. It returns a Future of Unit and that is all basically saying we are
   * throwing away this data at the end of the program.
   * @param allRequests All requests received from slate
   * @param ec This is the thread pool that Future are forked from.
   * @param db This is the database that we are interacting with.
   * @return
   */
  def ProcessRequests(allRequests: Seq[SlateEmergencyContactInfo])(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Unit] = {
    partitionToGroups(allRequests).flatMap {
      case (compliant, nonCompliant) =>
        dealWithCompliantRecords(compliant)
        dealWithNonCompliantRecords(nonCompliant)
    }

  }

  /**
   * Partition to groups takes all requests puts them through the parsingFunction toRows and then returns A tuple
   * of (Valid Rows to be inserted/updated, Invalid SlateEmergencyContactRequests for Manual Entry).
   *
   * @param allRequests These are the result of the SlateRequest entirely.
   * @param ec This is the thread pool that Future are forked from.
   * @param db This is the database that we are interacting with.
   * @return The tuple of valid and invalid data.
   */
  def partitionToGroups(allRequests: Seq[SlateEmergencyContactInfo])(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[(List[SpremrgRow], List[SlateEmergencyContactInfo])] = {
    toRows(allRequests).map(partitionFromEitherToGroups)
  }

  /**
   * This is the parsing Algorithm function. So first what we do is modify the Priorities. Then we go to the database
   * to get both the relationshipMap and the Pidm for that requests BannerID. Then we parse first name and last name,
   * get the appropriate relationshipCode for the entered Relationship. It ensures that valid records are parsed
   * phone numbers, and that the Postal Code is less than 30 because this form is ludicrously badly designed for
   * automation, and that they have a pidm. This returns either Left if it is Invalid or Right if it is valid.
   * @param records The set of Emergency Contact Info Records
   * @param ec The execution context to fork futures from
   * @param db The database to get information
   * @return A Future List of Either SlateEmergencyContactInfo not parsed, or a SpremrgRow that will be used to update
   *         the database.
   */
  private def toRows(records: Seq[SlateEmergencyContactInfo])(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[List[Either[SlateEmergencyContactInfo, SpremrgRow]]] = Future.sequence {
    for {
      record <- alterDataToCorrectPriorites(records)
    } yield for {
      pidm <- getPidmFromBannerID(record.BannerID)
      map <- generateRelationshipMap()
    } yield {

      val firstName = record.ECName.takeWhile(_ != ' ')
      val lastName = record.ECName.dropWhile(_ != ' ').drop(1)
      val relationshipCode = map.get(record.ECRelationship.trim.toUpperCase)

      val phone = parsePhone(record)
      val validZip = record.ECAddressPostal.map(_.length <= 30)

      val either = (pidm, phone, validZip, relationshipCode) match {
        case (Some(pid), Right(usPhoneNumber), Some(true), Some(code)) =>
          Right(
            SpremrgRow(
              pid,
              record.ECPriority.charAt(0),
              lastName,
              firstName,
              record.ECAddressStreet,
              record.ECAddressCity,
              record.ECAddressPostal,
              Some(usPhoneNumber.natnCode),
              usPhoneNumber.areaCode,
              Some(usPhoneNumber.phoneNumber),
              relationshipCode,
              new java.sql.Timestamp(new java.util.Date().getTime),
              Some("Slate Transfer"),
              Some("ECBATCH")
            )
          )
        case (_, _, _, _) =>
          Left(record)
      }

      either
    }
  }

  /**
   * This takes the list by bannerID and then changes The Priority List from wherever they happened to be in the form
   * to the appropriate number. This creates a List of Lists passes the individual lists to be parsed by
   * changePriorities and then returns a singular List as the call to changePriorities was flatMapped.
   *
   * @param seq The slate emergency contact information
   * @return An organized list of Emergency Contact Info
   */
  private def alterDataToCorrectPriorites(seq: Seq[SlateEmergencyContactInfo]): List[SlateEmergencyContactInfo] = {
    seq
      .groupBy(_.BannerID)
      .map(_._2.toList)
      .flatMap(changePriorities)
      .toList
  }

  /**
   * This changes random priority numbers for the individual. IE. Maybe contact 1, 3 => contact 1,2
   * ensuring they are at the top.
   * @param listGroupedByBannerID This is the list of emergency contacts for the individual student
   * @return A new list with the appropriate priority numbers.
   */
  private def changePriorities(listGroupedByBannerID: List[SlateEmergencyContactInfo]): List[SlateEmergencyContactInfo] = {

    @tailrec
    def internalChangePriorities(
      listGroupedByBannerID: List[SlateEmergencyContactInfo],
      recurse: Int,
      acc: List[SlateEmergencyContactInfo]
    ): List[SlateEmergencyContactInfo] = listGroupedByBannerID match {
      case Nil =>
        acc.reverse
      case x :: xs =>
        val newAcc = SlateEmergencyContactInfo(
          x.BannerID,
          recurse.toString,
          x.ECName,
          x.ECRelationship,
          x.ECCell,
          x.ECAddressStreet,
          x.ECAddressCity,
          x.ECAddressPostal
        ) :: acc

        internalChangePriorities(xs, recurse + 1, newAcc)
    }

    internalChangePriorities(listGroupedByBannerID, 1, List[SlateEmergencyContactInfo]())
  }

  /**
   * This algorithm takes the absolute mess that are phone numbers and tries to make them make sense sort of kind of.
   * This algorith really should only be trusted as far as you can throw it. And its digital if you catch my drift.
   *
   * Basically, We start by getting the Cell Number and removing +, and then replacing . and " "(spaces) with dashes(-)
   * We then attempt to parse this number
   *
   * USPhoneNumber - Starts with 1- , total length == 14
   * We take the area code as the first section after the nation code of 1
   * We take the rest of the number as the actual number
   * And then remove all the dashes because we are not animals.
   *
   * We are currently not utilizing the International Parser which makes sure it doesnt start with 1- and
   * that is less than 12 characters.
   *
   * @param contactInfo SlateEmergencyContact Information from Slate
   * @return Either a Left or Right on Number
   */
  private def parsePhone(contactInfo: SlateEmergencyContactInfo): Either[String, PhoneNumber] = {
    val number = contactInfo.ECCell.getOrElse("")
    val util = PhoneNumberUtil.getInstance()
    val phoneTry = Try(util.parse(number, "US")).toOption

    phoneTry match {
      case Some(phone) =>
        val countryCode = phone.getCountryCode.toString
        val nationalNumber = phone.getNationalNumber.toString

        countryCode match {
          case "1" =>
            val areaCode = nationalNumber.take(3)
            val number = nationalNumber.drop(3)
            if (number.length == 7) Right(PhoneNumber(countryCode, Some(areaCode), number))
            else Left(number)
          case _ => Right(PhoneNumber(countryCode, None, nationalNumber))
        }
      case None =>
        Left(number)

    }
  }

  /**
   * This transforms the List of Either[SEC, Spremrg] to a tuple of a
   * List of EmergencyContactInfo and a List of SpremrgRow
   * @param list The original list
   * @param ec The execution context to fork futures from
   * @param db The database to get information
   * @return A tuple of List of EmergencyContact Info and a List of SpremrgRows. These can be used for independent
   *         processing of the two types as 1 is for the database and 1 is for the email.
   */
  private def partitionFromEitherToGroups(list: List[Either[SlateEmergencyContactInfo, SpremrgRow]])(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): (List[SpremrgRow], List[SlateEmergencyContactInfo]) = {

    val partition = list.partition(_.isRight)
    val rowsForDB = partition._1.map(_.right.get)
    val rowsForManualEntry = partition._2.map(_.left.get)

    (rowsForDB, rowsForManualEntry)
  }

  /**
   * Uses the Update Database Function From DBFunctions to Update the state of the database with the new rows
   * to be processed
   * @param list List of Rows to be updated/inserted
   * @param ec The execution context to fork futures from
   * @param db The database to Write to
   * @return Unit You are lucky you know that anything happened.
   */
  private def dealWithCompliantRecords(list: List[SpremrgRow])(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Unit] = {
    UpdateDB(list)
  }

  /**
   * This is where we deal with Messy Records. First we parse them to a row in an HTML table and then we merge
   * that together into single string which we pass forward to Courier to deliver.
   * @param list The list of contact information
   * @param ec The execution context to fork futures from
   * @return A Future of Unit because it goes away.
   */
  private def dealWithNonCompliantRecords(list: List[SlateEmergencyContactInfo])(implicit ec: ExecutionContext): Future[Unit] = list.length match {
    case 0 => Future.successful((): Unit)
    case _ =>
      val content = list.map(printEmergencyContactInfoAsHTMLRow).foldRight("")(_ + _)
      Courier.sendManualParseEmail(content)
  }

  /**
   * Pretty print algorithm that is not used. But I'm not throwing away good code. It does make pretty strings from
   * the records that are easily readable in human terms.
   * @param slateEmergencyContactInfo The contact information to write
   * @return A string that is pretty.
   */
  def prettyPrintEmergencyContactInfo(slateEmergencyContactInfo: SlateEmergencyContactInfo): String = {
    s"""BannerID         - ${slateEmergencyContactInfo.BannerID}
        |Priority Number  - ${slateEmergencyContactInfo.ECPriority}
        |Name             - ${slateEmergencyContactInfo.ECName}
        |Relationship     - ${slateEmergencyContactInfo.ECRelationship}
        |Phone Number     - ${slateEmergencyContactInfo.ECCell.getOrElse("")}
        |Street Address   - ${slateEmergencyContactInfo.ECAddressStreet.getOrElse("")}
        |City             - ${slateEmergencyContactInfo.ECAddressCity.getOrElse("")}
        |Zip Code         - ${slateEmergencyContactInfo.ECAddressPostal.getOrElse("")}""".stripMargin
  }

  /**
   * Formulates The Emergency Contact information as a Row of an html table where each field is given a column to
   * be a part of. Any options are moved to empty strings if they are not present.
   * @param slateEmergencyContactInfo The contact information to write
   * @return A string ready to be placed in an html table with 8 columns
   */
  def printEmergencyContactInfoAsHTMLRow(slateEmergencyContactInfo: SlateEmergencyContactInfo): String = {
    s"""
       |<tr>
       |  <td>${slateEmergencyContactInfo.BannerID}</td>
       |  <td>${slateEmergencyContactInfo.ECPriority}</td>
       |  <td>${slateEmergencyContactInfo.ECName}</td>
       |  <td>${slateEmergencyContactInfo.ECRelationship}</td>
       |  <td>${slateEmergencyContactInfo.ECCell.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressStreet.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressCity.getOrElse("")}</td>
       |  <td>${slateEmergencyContactInfo.ECAddressPostal.getOrElse("")}</td>
       |</tr>
     """.stripMargin
  }

  /**
   * What it takes to be a phone number
   * @param natnCode A Nation Code Is Required
   * @param areaCode An area code is optional because we don't all live in the United States
   * @param phoneNumber A number
   */
  case class PhoneNumber(
    natnCode: String,
    areaCode: Option[String],
    phoneNumber: String
  )

}
