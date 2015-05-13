package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.querybuilder.QueryBuilder
import play.api.mvc.RequestHeader

import database.{ElasticSearch, Cassandra}
import constants.Table
import constants.PubSub._
import pubsub.{AnswerCreateMessage, PubSubHelper}
import utils.Alias

object Answer extends LongOrdered with DateOrdered with DoubleOrdered with Counter {
  def getList(ids: Seq[UUID], user: User = null) = {

    var userIds: Set[UUID] = Set()
    // get answers first
    val rows = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.ANSWER)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all()

    rows.foreach { row =>
      userIds += row.getUUID("creator_id")
    }

    val userActions = if(user != null) UserAction.get(user.id, "user", userIds.toSet) else Map[UUID, Map[String, String]]()
    val users = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.USER)
        .where(QueryBuilder.in("id", userIds.toSeq:_*))
    ).all().map { row =>
      (
        row.getUUID("id"),
        Map(
          "id" -> row.getUUID("id"),
          "alias" -> row.getString("alias"),
          "display" -> row.getString("display"),
          "followerCount" -> row.getLong("follower_count"),
          "actions" -> userActions.get(row.getUUID("id")).getOrElse(Map())
        )
      )
    }.toMap

    val actions = if(user != null) UserAction.get(user.id, "answer", ids.toSet) else Map[UUID, Map[String, String]]()

    val answers = rows.map { row =>
      (
        Map(
          "id" -> row.getUUID("id"),
          "creator" -> users(row.getUUID("creator_id")),
          "body" -> row.getString("body"),
          "shortBody" -> row.getString("short_body"),
          "updated" -> row.getDate("updated"),
          "voteUp" -> row.getLong("vote_up"),
          "voteDown" -> row.getLong("vote_down"),
          "vote" -> row.getLong("vote"),
          "score" -> row.getLong("score"),
          "confidence" -> row.getDouble("confidence"),
          "commentCount" -> row.getLong("comment_count"),
          "followerCount" -> row.getLong("follower_count"),
          "actions" -> actions.get(row.getUUID("id")).getOrElse(Map())
        )
      )
    }

    answers
  }

  def getHot(key: String, size: Int, next: (Double, UUID), user: User = null) = {
    val data = getDoubleIds(Table.ANSWER, "confidence", key, size, next, true)

    getList(data.map(_._1).toSeq, user)
  }

  def getTop(key: String, size: Int, next: (Long, UUID), user: User = null) = {
    val data = getLongIds(Table.ANSWER, "vote", key, size, next, true)

    getList(data.map(_._1).toSeq, user)
  }

  def getNewest(key: String, size: Int, next: (Long, UUID), user: User = null) = {
    val data = getDateIds(Table.ANSWER, "updated", key, size, next, true)

    getList(data.map(_._1).toSeq, user)
  }

  def create(body: String, questionId: UUID, creator: User)(implicit request: RequestHeader) = {
    // single field
    val id = UUIDs.timeBased()
    val created = new Date()
    val updated = new Date()
    val score = utils.Score.hot(0, 0, updated)
    val confidence = utils.Score.confidence(0, 0)

    Cassandra.session.execute(
      QueryBuilder.insertInto(Table.ANSWER)
        .value("id", id)
        .value("creator_id", creator.id)
        .value("question_id", questionId)
        .value("body", body)
        .value("created", created)
        .value("updated", updated)
        .value("status", 1)
        .value("vote_up", 0L)
        .value("vote_down", 0L)
        .value("vote", 0L)
        .value("score", score)
        .value("confidence", confidence)
        .value("comment_count", 0L)
    )

    val result = Map(
      "id" -> id,
      "creator" -> Map(
        "id" -> creator.id,
        "display" -> creator.display
      ),
      "body" -> body,
      "updated" -> updated,
      "voteUp" -> 0L,
      "voteDown" -> 0L,
      "vote" -> 0L,
      "score" -> score,
      "confidence" -> confidence,
      "commentCount" -> 0L,
      "followerCount" -> 0L,
      "actions" -> Map()
    )

    PubSubHelper.publish(TOPIC_ANSWER, AnswerCreateMessage(id, body, creator, created, updated, score, confidence, questionId))

    result
  }

  def getRow(input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.ANSWER)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }
}
