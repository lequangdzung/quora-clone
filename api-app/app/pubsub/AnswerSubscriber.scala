package pubsub

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import constants.PubSub._
import database.ElasticSearch
import models._
import utils.PrefixHelper
import constants.Table

case class AnswerCreateMessage(id: UUID, body: String, creator: User, created: Date, updated: Date, score: Long, confidence: Double, questionId: UUID)

class AnswerSubscriber extends Subscriber(TOPIC_ANSWER){
  def process(data: Any) = data match {
    case msg: AnswerCreateMessage => AnswerProcessor.create(msg)
  }
}

object AnswerProcessor extends Counter with LongOrdered with DateOrdered with DoubleOrdered{
  def create(message: AnswerCreateMessage) = {
    val keys = PrefixHelper.answerKeys(message.questionId, message.creator.id)
    // update score first

    longUpdate(Table.ANSWER, "score", message.id, message.score, keys, message.updated)
    dateUpdate(Table.ANSWER, "updated", message.id, message.updated, keys, message.updated)
    longUpdate(Table.ANSWER, "vote", message.id, 0L, keys, message.updated)
    doubleUpdate(Table.ANSWER, "confidence", message.id, message.confidence, keys, message.updated)


    incr(Table.QUESTION, "answer_count", message.questionId)

    UserAction.add(message.creator.id, "question", message.questionId, "answer", "0")

    PubSubHelper.publish(TOPIC_QUESTION, QuestionAddAnswerMessage(message.questionId, message.creator.id, message.id, message.updated))
    ElasticSearch.jsonIndex(
      constants.ElasticSearch.INDEX_TYPE_CONTENT,
      message.id.toString,
      Map(
        "body" -> message.body,
        "creator_id" -> message.creator.id,
        "created" -> message.created,
        "updated" -> message.updated,
        "question_id" -> message.questionId,
        "comment_count" -> 0,
        "vote_up" -> 0,
        "vote_down" -> 0,
        "vote" -> 0,
        "score" -> message.score
      )
    )
  }
}