package pubsub

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import constants.PubSub._
import database.{Cassandra, ElasticSearch}
import models._
import utils.{Score, PrefixHelper}
import constants.Table
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.Row


case class QuestionMessage(task: String, data: Map[String, Any], id: UUID, creatorId: UUID, topicIds: List[UUID], score: Long, updated: Date, vote: Long)
case class QuestionUpUserMessage(questionId: UUID, userId: UUID, updated: Date, row: Row)
case class QuestionDownUserMessage(questionId: UUID, userId: UUID, updated: Date, row: Row)

case class QuestionFollowMessage(questionId: UUID, userId: UUID, updated: Date)
case class QuestionAddAnswerMessage(questionId: UUID, userId: UUID, answerId: UUID, updated: Date)
case class QuestionVoteUpAnswerMessage(questionId: UUID, userId: UUID, answerId: UUID, updated: Date)

class QuestionSubscriber extends Subscriber(TOPIC_QUESTION){
  def process(data: Any) = data match {
    case msg: QuestionMessage if msg.task == TASK_CREATE => {
      QuestionProcessor.create(msg)
    }
    case msg: QuestionUpUserMessage => QuestionProcessor.upUser(msg)
    case msg: QuestionDownUserMessage => QuestionProcessor.downUser(msg)
    case msg: QuestionAddAnswerMessage => QuestionProcessor.QuestionAddAnswer(msg)
    case msg: QuestionVoteUpAnswerMessage => QuestionProcessor.QuestionVoteUpAnswer(msg)
  }
}

object QuestionProcessor extends Counter with LongOrdered with DateOrdered with Follow {
  def create(message: QuestionMessage) = {
    followTarget(message.creatorId, "question", message.id)

    QuestionUser.add(message.id, message.creatorId, "follow", message.updated, true)
    //val keys = PrefixHelper.questionKeys(message.creatorId, message.topicIds)
    val userKey = Set(PrefixHelper.getUserKey(message.creatorId))
    // update score first

    longUpdate(Table.QUESTION, "score", message.id, message.score, userKey, message.updated, "ask", "user", message.creatorId)
    dateUpdate(Table.QUESTION, "updated", message.id, message.updated, userKey, message.updated, "ask", "user", message.creatorId)

    message.topicIds.map { topicId =>
      val topicKey = Set(PrefixHelper.getTopicKey(topicId))
      longUpdate(Table.QUESTION, "score", message.id, message.score, topicKey, message.updated, "ask", "topic", topicId)
      dateUpdate(Table.QUESTION, "updated", message.id, message.updated, topicKey, message.updated, "ask", "topic", topicId)
    }

    longUpdate(Table.QUESTION, "score", message.id, message.score, userKey, message.updated, "ask", "user", message.creatorId)
    dateUpdate(Table.QUESTION, "updated", message.id, message.updated, userKey, message.updated, "ask", "user", message.creatorId)

    ElasticSearch.jsonIndex(constants.ElasticSearch.INDEX_TYPE_QUESTION, message.id.toString, message.data - "id")

    // update tag counter
    message.topicIds.foreach { topicId =>
      incr(Table.TOPIC, "question_count", topicId)
    }

    // update for followers of creator
    val followers = getFollowers("user", message.creatorId)
    val followerKeys = PrefixHelper.questionKeys(followers) ++ Set(PrefixHelper.questionFollowKey(message.creatorId))
    longUpdate(Table.QUESTION, "score", message.id, message.score, followerKeys, message.updated, "ask", "user", message.creatorId)
    dateUpdate(Table.QUESTION, "updated", message.id, message.updated, followerKeys, message.updated, "ask", "user", message.creatorId)

    // update for followers of topics
    message.topicIds.map { topicId =>
      val followers = getFollowers("topic", topicId) - message.creatorId
      val followerKeys = PrefixHelper.questionKeys(followers)
      longUpdate(Table.QUESTION, "score", message.id, message.score, followerKeys, message.updated, "ask", "topic", topicId)
      dateUpdate(Table.QUESTION, "updated", message.id, message.updated, followerKeys, message.updated, "ask", "topic", topicId)
    }
  }

  def follow(message: QuestionFollowMessage) = {
    // need to get question first
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.QUESTION)
        .where(QueryBuilder.eq("id", message.questionId))
    ).one()

    val followers = getFollowers("user", message.userId)
    val followerKeys = PrefixHelper.questionKeys(followers) ++ Set(PrefixHelper.questionFollowKey(message.userId))
    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated, "follow", "user", message.userId)
    dateUpdate(Table.QUESTION, "updated", message.questionId, row.getDate("updated"), followerKeys, message.updated, "follow", "user", message.userId)
  }

  def QuestionAddAnswer(message: QuestionAddAnswerMessage) = {
    // need to get question first
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.QUESTION)
        .where(QueryBuilder.eq("id", message.questionId))
    ).one()

    followTarget(message.userId, "question", message.questionId)

    val followers = getFollowers("user", message.userId)
    val followerKeys = PrefixHelper.questionKeys(followers) ++ Set(PrefixHelper.questionFollowKey(message.userId)) ++ Set(PrefixHelper.getUserKey(message.userId))
    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated, "answer", "user", message.userId)
    dateUpdate(Table.QUESTION, "updated", message.questionId, row.getDate("updated"), followerKeys, message.updated, "answer", "user", message.userId)
  }

  def QuestionVoteUpAnswer(message: QuestionVoteUpAnswerMessage) = {
    // need to get question first
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.QUESTION)
        .where(QueryBuilder.eq("id", message.questionId))
    ).one()

    val followers = getFollowers("user", message.userId)
    val followerKeys = PrefixHelper.questionKeys(followers) ++ Set(PrefixHelper.questionFollowKey(message.userId))
    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated, "voteup", "user", message.userId)
    dateUpdate(Table.QUESTION, "updated", message.questionId, row.getDate("updated"), followerKeys, message.updated, "voteup", "user", message.userId)
  }

  def upUser(message: QuestionUpUserMessage) = {
    val row = message.row
    val keys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), keys, message.updated)
    val followers = getFollowers("user", row.getUUID("creator_id"))
    val followerKeys = PrefixHelper.questionKeys(followers)

    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated)

    row.getList("topics", classOf[UUID]).toList.map { topicId =>
      val followers = getFollowers("topic", topicId)
      val followerKeys = PrefixHelper.questionKeys(followers)
      longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated)
    }
  }

  def downUser(message: QuestionDownUserMessage) = {
    val row = message.row
    val keys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), keys, message.updated)
    val followers = getFollowers("user", row.getUUID("creator_id"))
    val followerKeys = PrefixHelper.questionKeys(followers)

    longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated)

    row.getList("topics", classOf[UUID]).toList.map { topicId =>
      val followers = getFollowers("topic", topicId)
      val followerKeys = PrefixHelper.questionKeys(followers)
      longUpdate(Table.QUESTION, "score", message.questionId, row.getLong("score"), followerKeys, message.updated)
    }
  }
}