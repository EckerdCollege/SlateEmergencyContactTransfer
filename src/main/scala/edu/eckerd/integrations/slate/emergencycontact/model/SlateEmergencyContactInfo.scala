package edu.eckerd.integrations.slate.emergencycontact.model

/**
  * Created by davenpcm on 6/30/16.
  */
case class SlateEmergencyContactInfo(
                               BannerID: String,
                               ECPriority: String,
                               ECName: String,
                               ECRelationship: String,
                               ECCell: Option[String],
                               ECAddressStreet: Option[String],
                               ECAddressCity: Option[String],
                               ECAddressPostal: Option[String]
                               )
