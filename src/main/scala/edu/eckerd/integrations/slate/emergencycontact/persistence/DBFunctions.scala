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

package edu.eckerd.integrations.slate.emergencycontact.persistence

import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.Spremrg
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.SpremrgRow
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by davenpcm on 7/5/16.
 */
trait DBFunctions extends LazyLogging {
  val profile: slick.driver.JdbcProfile

  /**
   * This function is about as simple as they come. It takes a bannerID as a String, queries banner looking for a
   * Pidm using the gwf_get_pidm function, and then translates that to a BigDecimal Type which is the appropriate
   * column type for Pidms
   * @param bannerID The banner ID
   * @param ec The execution context to fork futures from
   * @param db The database to fetch information from
   * @return An option of a PIDM if the functioin returns one.
   */
  def getPidmFromBannerID(bannerID: String)(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Option[BigDecimal]] = {
    import profile.api._

    val id = bannerID.toUpperCase
    val action = sql"""SELECT gwf_get_pidm($id, 'E') from sys.dual""".as[Option[String]]
    val newAction = action.head
    db.run(newAction).map(_.map(BigDecimal(_)))
  }

  /**
   * This generates a Map from the description of the Relationship ie Mother, Father, Spouse to a code
   * value such as M, F , etc.
   * @param ec The execution context to fork futures from
   * @param db The database to fetch information from
   * @return This returns a Map of String -> Char
   */
  def generateRelationshipMap()(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Map[String, Char]] = {
    import profile.api._

    case class Relationship(code: String, description: Option[String])
    implicit val getRelationshipResult = GetResult(r => Relationship(r.<<, r.<<))

    val action = sql"""SELECT STVRELT_CODE, STVRELT_DESC FROM STVRELT""".as[Relationship]

    for {
      relationships <- db.run(action)
    } yield {
      val relationshipsFiltered = relationships.filter(_.description.isDefined)
      Map(relationshipsFiltered.map(r => r.description.get -> r.code.charAt(0)): _*)
    }
  }

  /**
   * Update The Database And Then Throw It away
   *
   * @param list List of All Users To Update
   * @param db Database to write To
   * @param ec Execution Context to Fork Processes Off Of
   * @return Unit. Fire and Forget On The Edge of The Application
   */
  def UpdateDB(list: List[SpremrgRow])(implicit db: JdbcProfile#Backend#Database, ec: ExecutionContext): Future[Unit] = {
    import profile.api._

    val actions = Future.sequence {
      for {
        row <- list
      } yield for {
        bool <- queryIfEmergencyContactExists(row)
        result <- bool match {
          case true =>
            logger.debug(s"Updating Row $row")
            updateByRow(row) recoverWith {
              case badKid =>
                logger.error(s"${badKid.getLocalizedMessage} at Pidm - ${row.pidm}, Priority - ${row.priority}")
                Future { badKid }
            }
          case false =>
            logger.debug(s"Inserting Row $row")
            db.run(Spremrg += row) recoverWith {
              case badKid =>
                logger.error(s"Error - ${badKid.getLocalizedMessage} at ${row.pidm}")
                Future { badKid }
            }
        }
      } yield result
    }

    actions.map(_ => ())
  }

  /**
   * This takes the rowItself and the performs a query to check if the rows primary key already exists in the
   * database. In this case that is Pidm and Priority Number. We exists to return a boolean
   * @param spremrgRow This is the row to check if exists
   * @param ec The execution context to fork futures from
   * @param db The database to check for information against.
   * @return A Boolean whether the record exists.
   */
  def queryIfEmergencyContactExists(spremrgRow: SpremrgRow)(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Boolean] = {
    import profile.api._
    val action = Spremrg.filter(row =>
      row.spremrgPidm === spremrgRow.pidm && row.spremrgPriority === spremrgRow.priority).exists.result

    action.statements.foreach(s => logger.debug(s"$s"))

    db.run(action)
  }

  /**
   * Same query by rather than querying for the result we create and Update to the database the returns the number
   * rows effected by the update, since this query is on the primary keys for the table that should only ever be 1.
   * @param spremrgRow The row to update
   * @param ec The execution context to fork futures from
   * @param db The database to update
   * @return An Integer representing the number of rows effected.
   */
  def updateByRow(spremrgRow: SpremrgRow)(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Int] = {
    import profile.api._
    val q = Spremrg.filter(row =>
      row.spremrgPidm === spremrgRow.pidm && row.spremrgPriority === spremrgRow.priority)
    val action = q.update(spremrgRow)
    db.run(action)
  }

}
