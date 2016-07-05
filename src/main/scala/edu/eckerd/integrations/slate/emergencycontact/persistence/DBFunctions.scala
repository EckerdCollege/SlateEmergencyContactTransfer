package edu.eckerd.integrations.slate.emergencycontact.persistence

import slick.driver.JdbcProfile
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
  * Created by davenpcm on 7/5/16.
  */
trait DBFunctions {
  val profile: slick.driver.JdbcProfile
  import profile.api._

  def getPidmFromBannerID(bannerID: String)(implicit ec: ExecutionContext, db: JdbcProfile#Backend#Database): Future[Option[String]] ={
    val id = bannerID.toUpperCase
    val action = sql"""SELECT gwf_get_pidm(${id}, 'E') from sys.dual""".as[Option[String]]
    val newaction = action.head
    db.run(newaction)
  }



}
