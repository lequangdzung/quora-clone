package pubsub

import constants.PubSub._
import models.v2.Mail
import utils.MailUtil

class MailSubscriber extends Subscriber(TOPIC_MAIL) {
  def process(data: Any) = data match {
    case mail: Mail => {
      //MailUtil.send(mail.subject, mail.content, mail.to.iterator().next())
      Mail.create(mail)
    }
    case _ =>
  }
}
