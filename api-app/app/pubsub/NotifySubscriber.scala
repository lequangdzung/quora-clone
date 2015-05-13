package pubsub

import constants.PubSub._
import java.util.UUID
import models.{User, Notify}
import security.oauth2.TokenStorage
import play.api.mvc.RequestHeader

case class GetNotifyData(userId: UUID, notifyTrackKeys: Set[String], limit: Int)
case class NotifyData(action: String, target_type: String, target_id: UUID, owner: UUID, title: String, link: String)

class NotifySubscriber extends Subscriber(TOPIC_NOTIFY){
  def process(data: Any) = data match {
    case getNotify: GetNotifyData =>
      Notify.resetNotify(getNotify.userId, getNotify.notifyTrackKeys, getNotify.limit)
      TokenStorage.resetNotify(getNotify.userId)
    case message: NotifyData => Notify.addNotify(message.action, message.target_type, message.target_id, message.owner, message.title, message.link)
  }
}
