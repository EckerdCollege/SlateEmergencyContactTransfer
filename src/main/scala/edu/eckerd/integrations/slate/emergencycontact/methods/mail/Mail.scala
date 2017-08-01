package edu.eckerd.integrations.slate.emergencycontact.methods.mail

case class Mail(
                 from: (String, String), // (email -> name)
                 to: Seq[String],
                 cc: Seq[String] = Seq.empty,
                 bcc: Seq[String] = Seq.empty,
                 subject: String,
                 message: String,
                 richMessage: Option[String] = None,
                 attachment: Option[(java.io.File)] = None
               ) {

  def send(hostname: String, user: String, pass: String): Unit = {
    import org.apache.commons.mail._

    val format =
      if (attachment.isDefined) MultiPart
      else if (richMessage.isDefined) Rich
      else Plain

    val commonsMail: Email = format match {
      case Plain => new SimpleEmail().setMsg(message)
      case Rich => new HtmlEmail().setHtmlMsg(richMessage.get).setTextMsg(message)
      case MultiPart => {
        val emailAttachmentN = new EmailAttachment()
        emailAttachmentN.setPath(attachment.get.getAbsolutePath)
        emailAttachmentN.setDisposition(EmailAttachment.ATTACHMENT)
        emailAttachmentN.setName(attachment.get.getName)
        new MultiPartEmail().attach(emailAttachmentN).setMsg(message)
      }
    }

    // TODO Set authentication from your configuration, sys properties or w/e

    // Can't add these via fluent API because it produces exceptions
    to foreach commonsMail.addTo
    cc foreach commonsMail.addCc
    bcc foreach commonsMail.addBcc

    commonsMail.setSmtpPort(587)
    commonsMail.setAuthenticator(new DefaultAuthenticator(user, pass))
    commonsMail.setHostName(hostname)
    commonsMail.getMailSession.getProperties.put("mail.smtps.auth", "true")
    commonsMail.getMailSession.getProperties.put("mail.smtps.port", "587")
    commonsMail.getMailSession.getProperties.put("mail.smtps.socketFactory.port", "587")
    commonsMail.getMailSession.getProperties.put("mail.smtps.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    commonsMail.getMailSession.getProperties.put("mail.smtps.socketFactory.fallback", "false")
    commonsMail.getMailSession.getProperties.put("mail.smtp.starttls.enable", "true")

    commonsMail
      .setFrom(from._1, from._2)
      .setSubject(subject)
      .send()
  }

}
