package models

import java.util.{UUID, Date}
import scala.collection.JavaConversions._

import com.datastax.driver.core.{Session, Cluster, querybuilder}
import com.datastax.driver.core.querybuilder._
import com.datastax.driver.core.utils._

import constants.PubSub._
import constants.Task._
import database.Cassandra
import pubsub._
import constants.Table
import scala.Some
import pubsub.UserData
import utils.{KeyHelper, Alias}
import play.api.mvc.RequestHeader

import models.v2.StringUnique

case class User(
  id: UUID,
  email: String,
  password: String,
  display: String,
  avatar: String,
  status: Boolean,
  permissions: Set[String],
  fbid: String,
  created: Date,
  followerCount: Long = 0L,
  unread: Long = 0L
)

object User extends Follow {
  def existUser(email: String) = {
    _findUser(email).isDefined
  }

  def getUser(email: String, password: String) = {
    _findUser(email).flatMap { id =>
      _getUser(id).filter(_.password == password)
    }
  }

  def addUser(email: String, password: String, display: String, fbid: String = "", orgAlias: String = null)(implicit request: RequestHeader) = {
    val userId = UUIDs.timeBased()
    val created = new Date
    val alias = if(orgAlias != null) orgAlias else Alias.convert(display, Alias.USER_KEY, "")

    val status = Boolean.box(false)
    val cql = QueryBuilder.insertInto(Table.USER)
      .value("id", userId)
      .value("created", created)
      .value("email", email)
      .value("display", display)
      .value("fbid", fbid)
      .value("alias", alias)
      .value("status", status)
      .value("password", password)
      .toString

    Cassandra.session.execute(cql)
    PubSubHelper.publish(TOPIC_ALIAS, AliasData(Alias.USER_KEY, alias, Map("id" -> userId, "display" -> display, "email" -> email)))
    PubSubHelper.publish(TOPIC_USER, UserData(CREATE, User(userId, email, password, display, "", false, Set(), fbid, created)))

    PubSubHelper.publish(TOPIC_ELATICSEAERCH, ElasticIndex(constants.ElasticSearch.INDEX_TYPE_USER, userId,
      Map(
        "created" -> created,
        "email" -> email,
        "display" -> display,
        "fbid" -> fbid,
        "alias" -> alias,
        "status" -> status,
        "password" -> password
      ), request
    ))

    User(userId, email, password, display, "", false, Set(), fbid, created)
  }

  def updatePassword(user: User, password: String) = {
    Cassandra.session.execute(
      QueryBuilder.update(Table.USER)
        .`with`(QueryBuilder.set("password", password))
        .where(QueryBuilder.eq("id", user.id))
    )
  }

  def _getUser(id: UUID): Option[User] = {
    val cql = QueryBuilder.select()
      .all()
      .from(Table.USER)
      .where(QueryBuilder.eq("id", id))
      .toString

    println("cql get user: " + cql)
    val row = Cassandra.session.execute(cql).one()

    val follows = getFollowTarget(id)

    if(row != null) {
      Some(User(
        row.getUUID("id"),
        row.getString("email"),
        row.getString("password"),
        row.getString("display"),
        row.getString("avatar"),
        row.getBool("status"),
        asScalaSet(row.getSet("permissions", classOf[java.lang.String])).toSet,
        row.getString("fbid"),
        row.getDate("created"),
        row.getLong("follower_count"),
        row.getLong("unread")
      ))
    }
    else None
  }

  def _findUser(email: String): Option[UUID] = {
    StringUnique.getTargetId("user", "email", email)
  }


  def getRow(input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.USER)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }

  def followQuestion(user: User, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID]) = {
    val result = followTarget(user.id, "question", questionId)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, FollowQuestionMessage(user.id, questionId, oldScore, updated, creatorId, tagIds))
    }

    result
  }

  def followTopic(follower: User, topicId: UUID, about: String = "follow") = {
    val result = followTarget(follower.id, "topic", topicId, about)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, FollowTopicMessage(follower, topicId, about))
    }

    result
  }

  def followUser(follower: User, targetUserId: UUID) = {
    val result = followTarget(follower.id, "user", targetUserId)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, FollowUserMessage(follower.id, targetUserId))
    }

    result
  }


  def unFollowQuestion(user: User, questionId: UUID, oldScore: Long, updated: Date, creatorId: UUID, tagIds: List[UUID]) = {
    val result = unFollowTarget(user.id, "question", questionId)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, UnFollowQuestionMessage(user.id, questionId, oldScore, updated, creatorId, tagIds))
    }

    result
  }


  def unFollowTopic(follower: User, topicId: UUID) = {
    val result = unFollowTarget(follower.id, "topic", topicId)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, UnFollowTopicMessage(follower, topicId))
    }

    result
  }

  def unFollowUser(follower: User, targetUserId: UUID) = {
    val result = unFollowTarget(follower.id, "user", targetUserId)
    if(result) {
      PubSubHelper.publish(TOPIC_USER, UnFollowUserMessage(follower.id, targetUserId))
    }

    result
  }

  def voteUp(user: User, target: String, target_id: UUID, voteUp: Long, voteDown: Long, score: Long, confidence: Double, updated: Date, parent: String, parent_id: UUID) = {
    val actions = UserAction.get(user.id, target, Set(target_id))
    val vote = actions.get(target_id).map(_.getOrElse("vote", null)).getOrElse(null)

    var result: Boolean = true
    if(vote == null) {
      UserAction.add(user.id, target, target_id, "vote", "up")

      PubSubHelper.publish(TOPIC_USER, VoteUpMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
    }
    else if(vote == "down") {
      UserAction.add(user.id, target, target_id, "vote", "up")
      PubSubHelper.publish(TOPIC_USER, DeleteVoteDownMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
      PubSubHelper.publish(TOPIC_USER, VoteUpMessage(user, target, target_id, updated, voteUp, voteDown - 1, score, confidence, parent, parent_id))
    }
    else result = false

    result
  }

  def deleteVoteUp(user: User, target: String, target_id: UUID, voteUp: Long, voteDown: Long, score: Long, confidence: Double, updated: Date, parent: String, parent_id: UUID) = {
    val actions = UserAction.get(user.id, target, Set(target_id))
    val vote = actions.get(target_id).map(_.getOrElse("vote", null)).getOrElse(null)

    var result: Boolean = true
    if(vote == "up") {
      UserAction.delete(user.id, target, target_id, "vote")
      PubSubHelper.publish(TOPIC_USER, DeleteVoteUpMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
    }
    else result = false

    result
  }

  def voteDown(user: User, target: String, target_id: UUID, voteUp: Long, voteDown: Long, score: Long, confidence: Double, updated: Date, parent: String, parent_id: UUID) = {
    val actions = UserAction.get(user.id, target, Set(target_id))
    val vote = actions.get(target_id).map(_.getOrElse("vote", null)).getOrElse(null)

    var result: Boolean = true
    if(vote == null) {
      UserAction.add(user.id, target, target_id, "vote", "down")

      PubSubHelper.publish(TOPIC_USER, VoteDownMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
    }
    else if(vote == "up") {
      UserAction.add(user.id, target, target_id, "vote", "down")
      PubSubHelper.publish(TOPIC_USER, VoteDownMessage(user, target, target_id, updated, voteUp - 1, voteDown, score, confidence, parent, parent_id))
      PubSubHelper.publish(TOPIC_USER, DeleteVoteUpMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
    }
    else result = false

    result
  }

  def deleteVoteDown(user: User, target: String, target_id: UUID, voteUp: Long, voteDown: Long, score: Long, confidence: Double, updated: Date, parent: String, parent_id: UUID) = {
    val actions = UserAction.get(user.id, target, Set(target_id))
    val vote = actions.get(target_id).map(_.getOrElse("vote", null)).getOrElse(null)

    var result: Boolean = true
    if(vote == "down") {
      UserAction.delete(user.id, target, target_id, "vote")
      PubSubHelper.publish(TOPIC_USER, DeleteVoteDownMessage(user, target, target_id, updated, voteUp, voteDown, score, confidence, parent, parent_id))
    }
    else result = false

    result
  }

  def getFollowTopics(user: User) = {
    val ids = getFollowTarget(user.id, "topic")

    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.TOPIC)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all().map { row =>
      val id = row.getUUID("id")
      Map(
        "id" -> id,
        "name" -> row.getString("name"),
        "followerCount" -> row.getLong("follower_count"),
        "questionCount" -> row.getLong("question_count")
      )
    }
  }

  def getFollowUsers(user: User) = {
    val ids = getFollowTarget(user.id, "user")

    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.USER)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all().map { row =>
      val id = row.getUUID("id")
      Map(
        "id" -> id,
        "alias" -> row.getString("alias"),
        "followerCount" -> row.getLong("follower_count")
      )
    }
  }

  def getFollowingUsers(user: User) = {
    val ids = getFollowers("user", user.id)

    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.USER)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all().map { row =>
      val id = row.getUUID("id")
      Map(
        "id" -> id,
        "alias" -> row.getString("alias"),
        "followerCount" -> row.getLong("follower_count")
      )
    }
  }
}