package pubsub

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import constants.PubSub._
import database.ElasticSearch
import models._
import utils.PrefixHelper
import constants.Table

case class CommentCreateMessage(id: UUID, body: String, creator: User, created: Date, updated: Date, confidence: Double, target: String, targetId: UUID)

class CommentSubscriber extends Subscriber(TOPIC_COMMENT){
  def process(data: Any) = data match {
    case msg: CommentCreateMessage => CommentProcessor.create(msg)
  }
}

object CommentProcessor extends Counter with LongOrdered with DateOrdered with DoubleOrdered{
  def create(message: CommentCreateMessage) = {
    val keys = PrefixHelper.targetKeys(message.target, message.targetId, message.creator.id)
    // update score first

    doubleUpdate(Table.COMMENT, "confidence", message.id, message.confidence, keys, message.updated)

    incr(message.target, "comment_count", message.targetId)

    UserAction.add(message.creator.id, message.target, message.targetId, "comment", "0")

    ElasticSearch.jsonIndex(
      constants.ElasticSearch.INDEX_TYPE_COMMENT,
      message.id.toString,
      Map(
        "body" -> message.body,
        "creator_id" -> message.creator.id,
        "created" -> message.created,
        "updated" -> message.updated,
        "target" -> message.target,
        "target_id" -> message.targetId,
        "comment_count" -> 0,
        "vote_up" -> 0,
        "vote_down" -> 0,
        "confidence" -> message.confidence
      )
    )

    Notify.addNotify("comment", message.target, message.targetId, message.creator.id, "", "")
  }
}