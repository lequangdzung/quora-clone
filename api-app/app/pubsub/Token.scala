package pubsub

import constants.PubSub._
import scalaoauth2.provider.{AuthInfo, AccessToken}
import models.v2.Token


class TokenSubscriber extends Subscriber(TOPIC_TOKEN) {
  def process(data: Any) = data match {
    case tk: Token => {
      Token.create(tk)
    }
    case _ =>
  }
}
