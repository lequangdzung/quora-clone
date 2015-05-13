package pubsub

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import constants.PubSub._
import database.ElasticSearch
import models._
import utils.PrefixHelper
import constants.Table

case class TopicCreateMessage(id: UUID, name: String, creator: User, created: Date, updated: Date, status: Boolean)

class TopicSubscriber extends Subscriber(TOPIC_TOPIC){
  def process(data: Any) = data match {
    case msg: TopicCreateMessage => TopicProcessor.create(msg)
  }
}

object TopicProcessor {
  def create(message: TopicCreateMessage) = {
    if(message.status) {
      User.followTopic(message.creator, message.id)
      ElasticSearch.jsonIndex(
        constants.ElasticSearch.INDEX_TYPE_TOPIC, message.id.toString,
        Map(
          "name" -> message.name,
          "created" -> message.created,
          "updated" -> message.updated,
          "status" -> message.status,
          "creator_id" -> message.creator.id
        )
      )
    }
  }
}