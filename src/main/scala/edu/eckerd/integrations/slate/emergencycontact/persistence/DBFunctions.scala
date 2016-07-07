package edu.eckerd.integrations.slate.emergencycontact.persistence

import com.typesafe.scalalogging.LazyLogging
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.Spremrg
import edu.eckerd.integrations.slate.emergencycontact.persistence.SPREMRG.SpremrgRow
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by davenpcm on 7/5/16.
  */
trait DBFunctions extends LazyLogging {
  val profile: slick.driver.JdbcProfile
  import profile.api._

  def getPidmFromBannerID(bannerID: String)
                         (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[Option[BigDecimal]] = {

    val id = bannerID.toUpperCase
    val action = sql"""SELECT gwf_get_pidm(${id}, 'E') from sys.dual""".as[Option[String]]
    val newAction = action.head
    db.run(newAction).map(_.map(BigDecimal(_)))
  }

  def generateRelationshipMap()
                             (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database)
  : Future[Map[String, Char]] = {

    case class Relationship(code: String, description: Option[String])
    implicit val getRelationshipResult = GetResult( r => Relationship(r.<<, r.<<))

    val action = sql"""SELECT STVRELT_CODE, STVRELT_DESC FROM STVRELT""".as[Relationship]

    for {
      relationships <- db.run(action)
    } yield {
      val relationshipsFiltered = relationships.filter(_.description.isDefined)
      Map( relationshipsFiltered.map(r => r.description.get -> r.code.charAt(0)) : _* )
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

    val actions = Future.sequence{
      for {
        row <- list
      } yield for {
        bool <- queryIfEmergencyContactExists(row)
        result <- bool match {
          case true =>
            logger.debug(s"Updating Row $row")
            updateByRow(row) recoverWith{
              case badKid =>
                logger.error(s"${badKid.getLocalizedMessage} at Pidm - ${row.pidm}, Priority - ${row.priority}")
                Future{badKid}
            }
          case false =>
            logger.debug(s"Inserting Row $row")
            db.run(Spremrg += row) recoverWith{
              case badKid =>
                logger.error(s"Error - ${badKid.getLocalizedMessage} at ${row.pidm}")
                Future{badKid}
            }
        }
      } yield result
    }


    actions.map(_ => ())
  }

  def queryIfEmergencyContactExists(spremrgRow: SpremrgRow)
                                   (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Boolean] = {
    import profile.api._
    val action = Spremrg.filter(row =>
      row.spremrgPidm === spremrgRow.pidm && row.spremrgPriority === spremrgRow.priority
    ).exists.result

    db.run(action)
  }

  def updateByRow(spremrgRow: SpremrgRow)
                 (implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Int] = {
    import profile.api._
    val q = Spremrg.filter(row =>
      row.spremrgPidm === spremrgRow.pidm && row.spremrgPriority === spremrgRow.priority
    )
    val action = q.update(spremrgRow)
    db.run(action)
  }

}
