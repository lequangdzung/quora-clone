package pubsub

import constants.PubSub._
import scalaoauth2.provider.{AuthInfo, AccessToken}
import models.{Token, User}

case class TokenData(task: String, token: AccessToken, auth: AuthInfo[User])

class AccessTokenSubscriber extends Subscriber(TOPIC_ACCESS_TOKEN) {
  def process(data: Any) = data match {
    case t: TokenData => {
      Token.storeToken(t.token, t.auth)
    }
    case _ =>
  }
}
