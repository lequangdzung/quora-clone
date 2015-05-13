package pubsub

import constants.PubSub._
import java.util.UUID
import score.UserScore

import utils.Alias

case class AliasData(key: String, alias: String, data: Map[String, Any])

class AliasSubscriber extends Subscriber(TOPIC_ALIAS){
  def process(data: Any) = data match {
    case data: AliasData => Alias.save(data.key, data.alias, data.data)
  }
}