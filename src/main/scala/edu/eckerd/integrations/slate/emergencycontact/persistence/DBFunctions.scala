package edu.eckerd.integrations.slate.emergencycontact.persistence

import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by davenpcm on 7/5/16.
  */
trait DBFunctions {
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

}
