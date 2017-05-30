package edu.eckerd.integrations.slate.emergencycontact.methods.mail

sealed trait MailType
case object Plain extends MailType
case object Rich extends MailType
case object MultiPart extends MailType