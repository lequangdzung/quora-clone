package utils

import scala.collection.JavaConversions._
import courier._, Defaults._

import constants.Mail._
import javax.mail.internet.InternetAddress
import models.v2.Mail

object MailUtil {
  val mailer = Mailer(HOST, PORT)
    .auth(AUTH)
    .as(USER, PASS)
    .startTtls(TTLS)()

  def send(subject: String, content: String, to: String) = {
    println("mailer is: " + mailer)

    println("from is: " + FROM)
    mailer(Envelope.from(new InternetAddress(FROM))
      .to(new InternetAddress(to))
      .subject(subject)
      .content(Text(content))).onSuccess {
        case _ => println("message delivered")
      }
  }

  def send(mail: Mail) = {
    mailer(Envelope.from(new InternetAddress(FROM))
      .to(mail.to.map(new InternetAddress(_)).toSeq:_*)
      .subject(mail.subject)
      .content(Multipart().html(mail.content))
      ).onSuccess {
        case _ => {
          Mail.updateDeliver(mail)
          println("message delivered")
        }
      }
  }
}
