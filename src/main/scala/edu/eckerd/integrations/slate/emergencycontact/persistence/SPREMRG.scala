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

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SPREMRG extends {
  val profile = com.typesafe.slick.driver.oracle.OracleDriver
} with SPREMRG

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SPREMRG {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{ GetResult => GR }

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Spremrg.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  case class SpremrgRow(
    pidm: BigDecimal,
    priority: Char,
    lastName: String,
    firstName: String,
    streetAddr: Option[String],
    city: Option[String],
    zip: Option[String],
    phoneCountryCode: Option[String],
    phoneAreaCode: Option[String],
    phoneNumber: Option[String],
    relationshipCode: Option[Char],
    activityDate: java.sql.Timestamp,
    dataOrigin: Option[String],
    userId: Option[String]
  )

  /** Table description of table SPREMRG. Objects of this class serve as prototypes for rows in queries. */
  class Spremrg(_tableTag: Tag) extends Table[SpremrgRow](_tableTag, Some("SATURN"), "SPREMRG") {

    def * = (
      spremrgPidm,
      spremrgPriority,
      spremrgLastName,
      spremrgFirstName,
      spremrgStreetLine1,
      spremrgCity,
      spremrgZip,
      spremrgCtryCodePhone,
      spremrgPhoneArea,
      spremrgPhoneNumber,
      spremrgReltCode,
      spremrgActivityDate,
      spremrgDataOrigin,
      spremrgUserId
    ) <> (SpremrgRow.tupled, SpremrgRow.unapply)

    /** Database column SPREMRG_PIDM SqlType(NUMBER) */
    val spremrgPidm: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("SPREMRG_PIDM")
    /** Database column SPREMRG_PRIORITY SqlType(VARCHAR2) */
    val spremrgPriority: Rep[Char] = column[Char]("SPREMRG_PRIORITY")
    /** Database column SPREMRG_LAST_NAME SqlType(VARCHAR2), Length(60,true) */
    val spremrgLastName: Rep[String] = column[String]("SPREMRG_LAST_NAME", O.Length(60, varying = true))
    /** Database column SPREMRG_FIRST_NAME SqlType(VARCHAR2), Length(60,true) */
    val spremrgFirstName: Rep[String] = column[String]("SPREMRG_FIRST_NAME", O.Length(60, varying = true))
    //    /** Database column SPREMRG_MI SqlType(VARCHAR2), Length(60,true) */
    //    val spremrgMi: Rep[Option[String]] = column[Option[String]]("SPREMRG_MI", O.Length(60,varying=true))
    /** Database column SPREMRG_STREET_LINE1 SqlType(VARCHAR2), Length(75,true) */
    val spremrgStreetLine1: Rep[Option[String]] = column[Option[String]]("SPREMRG_STREET_LINE1", O.Length(75, varying = true))
    //    /** Database column SPREMRG_STREET_LINE2 SqlType(VARCHAR2), Length(75,true) */
    //    val spremrgStreetLine2: Rep[Option[String]] = column[Option[String]]("SPREMRG_STREET_LINE2", O.Length(75,varying=true))
    //    /** Database column SPREMRG_STREET_LINE3 SqlType(VARCHAR2), Length(75,true) */
    //    val spremrgStreetLine3: Rep[Option[String]] = column[Option[String]]("SPREMRG_STREET_LINE3", O.Length(75,varying=true))
    /** Database column SPREMRG_CITY SqlType(VARCHAR2), Length(50,true) */
    val spremrgCity: Rep[Option[String]] = column[Option[String]]("SPREMRG_CITY", O.Length(50, varying = true))
    //    /** Database column SPREMRG_STAT_CODE SqlType(VARCHAR2), Length(3,true) */
    //    val spremrgStatCode: Rep[Option[String]] = column[Option[String]]("SPREMRG_STAT_CODE", O.Length(3,varying=true))
    //    /** Database column SPREMRG_NATN_CODE SqlType(VARCHAR2), Length(5,true) */
    //    val spremrgNatnCode: Rep[Option[String]] = column[Option[String]]("SPREMRG_NATN_CODE", O.Length(5,varying=true))
    /** Database column SPREMRG_ZIP SqlType(VARCHAR2), Length(30,true) */
    val spremrgZip: Rep[Option[String]] = column[Option[String]]("SPREMRG_ZIP", O.Length(30, varying = true))
    /** Database column SPREMRG_PHONE_AREA SqlType(VARCHAR2), Length(6,true) */
    val spremrgPhoneArea: Rep[Option[String]] = column[Option[String]]("SPREMRG_PHONE_AREA", O.Length(6, varying = true))
    /** Database column SPREMRG_PHONE_NUMBER SqlType(VARCHAR2), Length(12,true) */
    val spremrgPhoneNumber: Rep[Option[String]] = column[Option[String]]("SPREMRG_PHONE_NUMBER", O.Length(12, varying = true))
    /** Database column SPREMRG_PHONE_EXT SqlType(VARCHAR2), Length(10,true) */
    //    val spremrgPhoneExt: Rep[Option[String]] = column[Option[String]]("SPREMRG_PHONE_EXT", O.Length(10,varying=true))
    //    /** Database column SPREMRG_RELT_CODE SqlType(VARCHAR2) */
    val spremrgReltCode: Rep[Option[Char]] = column[Option[Char]]("SPREMRG_RELT_CODE")
    /** Database column SPREMRG_ACTIVITY_DATE SqlType(DATE) */
    val spremrgActivityDate: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("SPREMRG_ACTIVITY_DATE")
    //    /** Database column SPREMRG_ATYP_CODE SqlType(VARCHAR2), Length(2,true) */
    //    val spremrgAtypCode: Rep[Option[String]] = column[Option[String]]("SPREMRG_ATYP_CODE", O.Length(2,varying=true))
    /** Database column SPREMRG_DATA_ORIGIN SqlType(VARCHAR2), Length(30,true) */
    val spremrgDataOrigin: Rep[Option[String]] = column[Option[String]]("SPREMRG_DATA_ORIGIN", O.Length(30, varying = true))
    /** Database column SPREMRG_USER_ID SqlType(VARCHAR2), Length(30,true) */
    val spremrgUserId: Rep[Option[String]] = column[Option[String]]("SPREMRG_USER_ID", O.Length(30, varying = true))
    //    /** Database column SPREMRG_CNTY_CODE SqlType(VARCHAR2), Length(5,true) */
    //    val spremrgCntyCode: Rep[Option[String]] = column[Option[String]]("SPREMRG_CNTY_CODE", O.Length(5,varying=true))
    //    /** Database column SPREMRG_DELIVERY_POINT SqlType(VARCHAR2), Length(2,true) */
    //    val spremrgDeliveryPoint: Rep[Option[String]] = column[Option[String]]("SPREMRG_DELIVERY_POINT", O.Length(2,varying=true))
    //    /** Database column SPREMRG_CORRECTION_DIGIT SqlType(VARCHAR2) */
    //    val spremrgCorrectionDigit: Rep[Option[Char]] = column[Option[Char]]("SPREMRG_CORRECTION_DIGIT")
    //    /** Database column SPREMRG_CARRIER_ROUTE SqlType(VARCHAR2), Length(4,true) */
    //    val spremrgCarrierRoute: Rep[Option[String]] = column[Option[String]]("SPREMRG_CARRIER_ROUTE", O.Length(4,varying=true))
    //    /** Database column SPREMRG_REVIEWED_IND SqlType(VARCHAR2) */
    //    val spremrgReviewedInd: Rep[Option[Char]] = column[Option[Char]]("SPREMRG_REVIEWED_IND")
    //    /** Database column SPREMRG_REVIEWED_USER SqlType(VARCHAR2), Length(30,true) */
    //    val spremrgReviewedUser: Rep[Option[String]] = column[Option[String]]("SPREMRG_REVIEWED_USER", O.Length(30,varying=true))
    //    /** Database column SPREMRG_SURNAME_PREFIX SqlType(VARCHAR2), Length(60,true) */
    //    val spremrgSurnamePrefix: Rep[Option[String]] = column[Option[String]]("SPREMRG_SURNAME_PREFIX", O.Length(60,varying=true))
    /** Database column SPREMRG_CTRY_CODE_PHONE SqlType(VARCHAR2), Length(4,true) */
    val spremrgCtryCodePhone: Rep[Option[String]] = column[Option[String]]("SPREMRG_CTRY_CODE_PHONE", O.Length(4, varying = true))
    //    /** Database column SPREMRG_HOUSE_NUMBER SqlType(VARCHAR2), Length(10,true) */
    //    val spremrgHouseNumber: Rep[Option[String]] = column[Option[String]]("SPREMRG_HOUSE_NUMBER", O.Length(10,varying=true))
    //    /** Database column SPREMRG_STREET_LINE4 SqlType(VARCHAR2), Length(75,true) */
    //    val spremrgStreetLine4: Rep[Option[String]] = column[Option[String]]("SPREMRG_STREET_LINE4", O.Length(75,varying=true))
    //    /** Database column SPREMRG_SURROGATE_ID SqlType(NUMBER) */
    //    val spremrgSurrogateId: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("SPREMRG_SURROGATE_ID")
    //    /** Database column SPREMRG_VERSION SqlType(NUMBER) */
    //    val spremrgVersion: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("SPREMRG_VERSION")
    //    /** Database column SPREMRG_VPDI_CODE SqlType(VARCHAR2), Length(6,true) */
    //    val spremrgVpdiCode: Rep[Option[String]] = column[Option[String]]("SPREMRG_VPDI_CODE", O.Length(6,varying=true))

    /** Primary key of Spremrg (database name PK_SPREMRG) */
    val pk = primaryKey("PK_SPREMRG", (spremrgPidm, spremrgPriority))
  }
  /** Collection-like TableQuery object for table Spremrg */
  lazy val Spremrg = new TableQuery(tag => new Spremrg(tag))
}
