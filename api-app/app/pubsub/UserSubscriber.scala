package pubsub

import scala.collection.JavaConversions._

import constants.PubSub._
import models._
import java.util.{Date, UUID}
import utils.{PrefixHelper, Score, KeyHelper}

import models.v2.StringUnique
import constants.Table
import database.Cassandra
import com.datastax.driver.core.querybuilder.QueryBuilder

case class UserData(task: String, user: User)

case class FollowQuestionMessage(userId: UUID, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID])
case class FollowTopicMessage(follower: User, topicId: UUID, about: String)
case class FollowUserMessage(userId: UUID, targetUserId: UUID)

case class UnFollowQuestionMessage(userId: UUID, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID])
case class UnFollowTopicMessage(follower: User, topicId: UUID)
case class UnFollowUserMessage(userId: UUID, targetUserId: UUID)

case class VoteUpMessage(user: User, target: String, target_id: UUID, updated: Date, voteUp: Long, voteDown: Long, score: Long, confidence: Double, parent: String, parent_id: UUID)
case class DeleteVoteUpMessage(user: User, target: String, target_id: UUID, updated: Date, voteUp: Long, voteDown: Long, score: Long, confidence: Double, parent: String, parent_id: UUID)
case class VoteDownMessage(user: User, target: String, target_id: UUID, updated: Date, voteUp: Long, voteDown: Long, score: Long, confidence: Double, parent: String, parent_id: UUID)
case class DeleteVoteDownMessage(user: User, target: String, target_id: UUID, updated: Date, voteUp: Long, voteDown: Long, score: Long, confidence: Double, parent: String, parent_id: UUID)

class UserSubscriber extends Subscriber(TOPIC_USER){
  def process(data: Any) = data match {
    case u: UserData => {
      StringUnique.create(StringUnique("user_email", u.user.email, u.user.id))
      //AkkaHelper.sendOut(remoteakka.CreateUserGraph(u.user.id, u.user.email, u.user.display, new Date))
    }
    case message: FollowQuestionMessage =>  UserProcessor.followQuestion(message.userId, message.questionId, message.oldScore, message.updated, message.creatorId, message.tagIds)
    case message: FollowTopicMessage =>  UserProcessor.followTopic(message.follower.id, message.topicId)
    case message: FollowUserMessage =>  UserProcessor.followUser(message.userId, message.targetUserId)

    case message: UnFollowQuestionMessage =>  UserProcessor.unFollowQuestion(message.userId, message.questionId, message.oldScore, message.updated, message.creatorId, message.tagIds)
    case message: UnFollowTopicMessage =>  UserProcessor.unFollowTopic(message.follower.id, message.topicId)
    case message: UnFollowUserMessage =>  UserProcessor.unFollowUser(message.userId, message.targetUserId)

    case message: VoteUpMessage => UserProcessor.voteUp(message)
    case message: DeleteVoteUpMessage => UserProcessor.deleteVoteUp(message)
    case message: VoteDownMessage => UserProcessor.voteDown(message)
    case message: DeleteVoteDownMessage => UserProcessor.deleteVoteDown(message)

  }
}

object UserProcessor extends Counter with LongOrdered with DoubleOrdered with DateOrdered with Follow {
  def followQuestion(userId: UUID, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID]) = {
    // update question score
    incr(Table.QUESTION, "follower_count", questionId)

    QuestionUser.add(questionId, userId, "follow", updated)
    QuestionProcessor.follow(QuestionFollowMessage(questionId, userId, updated))
  }

  def followTopic(userId: UUID, topicId: UUID) = {
    incr(Table.TOPIC, "follower_count", topicId)

    // copy the first 50 rows
    val topicKey = PrefixHelper.getTopicKey(topicId)
    val userKey = PrefixHelper.getUserKey(userId)

    longCopy("question_score_" + topicKey, "question_score_" + userKey)
    dateCopy("question_updated_" + topicKey, "question_updated_" + userKey)
    dateCopy("question_confidence_" + topicKey, "question_confidence_" + userKey)

  }

  def followUser(userId: UUID, targetUserId: UUID) = {
    incr(Table.USER, "follower_count", targetUserId)

    // copy the first 50 rows
    val followerKey = PrefixHelper.questionFollowKey(targetUserId)
    val userKey = PrefixHelper.getUserKey(userId)

    longCopy("question_score_" + followerKey, "question_score_" + userKey)
    dateCopy("question_updated_" + followerKey, "question_updated_" + userKey)
    dateCopy("question_confidence_" + followerKey, "question_confidence_" + userKey)
  }

  def unFollowQuestion(userId: UUID, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID]) = {
    // update question score
    decr(Table.QUESTION, "follower_count", questionId)
    QuestionUser.delete(questionId, userId, "follow", updated)
  }

  def unFollowTopic(userId: UUID, topicId: UUID) = {
    decr(Table.TOPIC, "follower_count", topicId)
  }

  def unFollowUser(userId: UUID, targetUserId: UUID) = {
    decr(Table.USER, "follower_count", targetUserId)
  }

  def voteUp(msg: VoteUpMessage) = {
    val voteUp = incr(msg.target, "vote_up", msg.target_id)
    val newScore = utils.Score.hot(voteUp, msg.voteDown, msg.updated)

    val scoreStep = newScore - msg.score


    incr(msg.target, "score", msg.target_id, scoreStep)

    val keys = PrefixHelper.targetKeys(msg.parent, msg.parent_id, msg.user.id)
    this.synchronized {
      // update confidence need to only use on thread
      val newConfidence = utils.Score.confidence(voteUp, msg.voteDown)

      Cassandra.session.execute(
        QueryBuilder.update(msg.target)
          .`with`(QueryBuilder.set("confidence", newConfidence))
          .where(QueryBuilder.eq("id", msg.target_id))
      )
      doubleUpdate(msg.target, "confidence", msg.target_id, newConfidence, keys, msg.updated)
      QuestionUser.add(msg.parent_id, msg.user.id, "voteup", msg.updated)

      if(msg.parent == "question") {
        val doubleRow = getDoubleTopRow(Table.ANSWER, "confidence", "question_" + msg.parent_id)
        val answerId = doubleRow.getUUID("id")

        val confidence = doubleRow.getDouble("value")
        Cassandra.session.execute(
          QueryBuilder.update(Table.QUESTION)
            .`with`(QueryBuilder.set("confidence", confidence))
            .where(QueryBuilder.eq("id", msg.parent_id))
        )

        val row = Cassandra.session.execute(
          QueryBuilder.select().all().from(Table.QUESTION)
            .where(QueryBuilder.eq("id", msg.parent_id))
        ).one()

        var questionKeys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        val followers = getFollowers("user", row.getUUID("creator_id"))
        questionKeys = questionKeys ++ PrefixHelper.questionKeys(followers)

        row.getList("topics", classOf[UUID]).map { topicId =>
          val topicFollowers = getFollowers("topic", topicId)
          questionKeys = questionKeys ++ PrefixHelper.questionKeys(topicFollowers)
        }
        doubleUpdate("question", "confidence", msg.parent_id, confidence, questionKeys, msg.updated, null, null, null, answerId)
        PubSubHelper.publish(TOPIC_QUESTION, QuestionVoteUpAnswerMessage(msg.parent_id, msg.user.id, msg.target_id, msg.updated))
      }
    }

    longUpdate(msg.target, "score", msg.target_id, newScore, keys, msg.updated)
    longUpdate(msg.target, "vote", msg.target_id, voteUp - msg.voteDown, keys, msg.updated)
    Notify.addNotify("voteup", msg.target, msg.target_id, msg.user.id, "", "")
  }

  def deleteVoteUp(msg: DeleteVoteUpMessage) = {
    val voteUp = decr(msg.target, "vote_up", msg.target_id)
    val newScore = utils.Score.hot(voteUp, msg.voteDown, msg.updated)

    val scoreStep = newScore - msg.score


    incr(msg.target, "score", msg.target_id, scoreStep)

    val keys = PrefixHelper.targetKeys(msg.parent, msg.parent_id, msg.user.id)
    this.synchronized {
      // update confidence need to only use on thread
      val newConfidence = utils.Score.confidence(voteUp, msg.voteDown)
      Cassandra.session.execute(
        QueryBuilder.update(msg.target)
          .`with`(QueryBuilder.set("confidence", newConfidence))
          .where(QueryBuilder.eq("id", msg.target_id))
      )
      doubleUpdate(msg.target, "confidence", msg.target_id, newConfidence, keys, msg.updated)

      if(msg.parent == "question") {
        val doubleRow = getDoubleTopRow(Table.ANSWER, "confidence", "question_" + msg.parent_id)
        val answerId = doubleRow.getUUID("id")

        val confidence = doubleRow.getDouble("value")
        Cassandra.session.execute(
          QueryBuilder.update(Table.QUESTION)
            .`with`(QueryBuilder.set("confidence", confidence))
            .where(QueryBuilder.eq("id", msg.parent_id))
        )

        val row = Cassandra.session.execute(
          QueryBuilder.select().all().from(Table.QUESTION)
            .where(QueryBuilder.eq("id", msg.parent_id))
        ).one()

        var questionKeys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        val followers = getFollowers("user", row.getUUID("creator_id"))
        questionKeys = questionKeys ++ PrefixHelper.questionKeys(followers)

        row.getList("topics", classOf[UUID]).map { topicId =>
          val topicFollowers = getFollowers("topic", topicId)
          questionKeys = questionKeys ++ PrefixHelper.questionKeys(topicFollowers)
        }
        doubleUpdate("question", "confidence", msg.parent_id, confidence, questionKeys, msg.updated, null, null, null, answerId)
      }
    }

    longUpdate(msg.target, "score", msg.target_id, newScore, keys, msg.updated)
    longUpdate(msg.target, "vote", msg.target_id, voteUp - msg.voteDown, keys, msg.updated)
    QuestionUser.delete(msg.parent_id, msg.user.id, "deletevoteup", msg.updated)

    Notify.addNotify("deletevoteup", msg.target, msg.target_id, msg.user.id, "", "")
  }

  def voteDown(msg: VoteDownMessage) = {
    val voteDown = incr(msg.target, "vote_down", msg.target_id)

    val newScore = utils.Score.hot(msg.voteUp, voteDown, msg.updated)

    val scoreStep = newScore - msg.score


    incr(msg.target, "score", msg.target_id, scoreStep)

    val keys = PrefixHelper.targetKeys(msg.parent, msg.parent_id, msg.user.id)
    this.synchronized {
      // update confidence need to only use on thread
      val newConfidence = utils.Score.confidence(msg.voteUp, voteDown)
      Cassandra.session.execute(
        QueryBuilder.update(msg.target)
          .`with`(QueryBuilder.set("confidence", newConfidence))
          .where(QueryBuilder.eq("id", msg.target_id))
      )
      doubleUpdate(msg.target, "confidence", msg.target_id, newConfidence, keys, msg.updated)
      if(msg.parent == "question") {
        val doubleRow = getDoubleTopRow(Table.ANSWER, "confidence", "question_" + msg.parent_id)
        val answerId = doubleRow.getUUID("id")

        val confidence = doubleRow.getDouble("value")
        Cassandra.session.execute(
          QueryBuilder.update(Table.QUESTION)
            .`with`(QueryBuilder.set("confidence", confidence))
            .where(QueryBuilder.eq("id", msg.parent_id))
        )

        val row = Cassandra.session.execute(
          QueryBuilder.select().all().from(Table.QUESTION)
            .where(QueryBuilder.eq("id", msg.parent_id))
        ).one()

        var questionKeys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        val followers = getFollowers("user", row.getUUID("creator_id"))
        questionKeys = questionKeys ++ PrefixHelper.questionKeys(followers)

        row.getList("topics", classOf[UUID]).map { topicId =>
          val topicFollowers = getFollowers("topic", topicId)
          questionKeys = questionKeys ++ PrefixHelper.questionKeys(topicFollowers)
        }
        doubleUpdate("question", "confidence", msg.parent_id, confidence, questionKeys, msg.updated, null, null, null, answerId)
      }
    }

    longUpdate(msg.target, "score", msg.target_id, newScore, keys, msg.updated)
    longUpdate(msg.target, "vote", msg.target_id, msg.voteUp - voteDown, keys, msg.updated)

    QuestionUser.add(msg.parent_id, msg.user.id, "votedown", msg.updated)
    Notify.addNotify("votedown", msg.target, msg.target_id, msg.user.id, "", "")
  }

  def deleteVoteDown(msg: DeleteVoteDownMessage) = {
    val voteDown = decr(msg.target, "vote_down", msg.target_id)
    val newScore = utils.Score.hot(msg.voteUp, voteDown, msg.updated)

    val scoreStep = newScore - msg.score


    incr(msg.target, "score", msg.target_id, scoreStep)

    val keys = PrefixHelper.targetKeys(msg.parent, msg.parent_id, msg.user.id)
    this.synchronized {
      // update confidence need to only use on thread
      val newConfidence = utils.Score.confidence(msg.voteUp, voteDown)
      Cassandra.session.execute(
        QueryBuilder.update(msg.target)
          .`with`(QueryBuilder.set("confidence", newConfidence))
          .where(QueryBuilder.eq("id", msg.target_id))
      )
      doubleUpdate(msg.target, "confidence", msg.target_id, newConfidence, keys, msg.updated)

      if(msg.parent == "question") {
        val doubleRow = getDoubleTopRow(Table.ANSWER, "confidence", "question_" + msg.parent_id)
        val answerId = doubleRow.getUUID("id")

        val confidence = doubleRow.getDouble("value")
        Cassandra.session.execute(
          QueryBuilder.update(Table.QUESTION)
            .`with`(QueryBuilder.set("confidence", confidence))
            .where(QueryBuilder.eq("id", msg.parent_id))
        )

        val row = Cassandra.session.execute(
          QueryBuilder.select().all().from(Table.QUESTION)
            .where(QueryBuilder.eq("id", msg.parent_id))
        ).one()

        var questionKeys = PrefixHelper.questionKeys(row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        val followers = getFollowers("user", row.getUUID("creator_id"))
        questionKeys = questionKeys ++ PrefixHelper.questionKeys(followers)

        row.getList("topics", classOf[UUID]).map { topicId =>
          val topicFollowers = getFollowers("topic", topicId)
          questionKeys = questionKeys ++ PrefixHelper.questionKeys(topicFollowers)
        }
        doubleUpdate("question", "confidence", msg.parent_id, confidence, questionKeys, msg.updated, null, null, null, answerId)
      }
    }

    longUpdate(msg.target, "score", msg.target_id, newScore, keys, msg.updated)
    longUpdate(msg.target, "vote", msg.target_id, msg.voteUp - voteDown, keys, msg.updated)
    QuestionUser.delete(msg.parent_id, msg.user.id, "deletevotedown", msg.updated)
    Notify.addNotify("deletevotedown", msg.target, msg.target_id, msg.user.id, "", "")
  }
}