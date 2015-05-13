package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.querybuilder.QueryBuilder
import play.api.mvc.RequestHeader

import database.{ElasticSearch, Cassandra}
import constants.Table
import constants.PubSub._
import pubsub._
import utils.Alias
import scala.Some

object Comment extends LongOrdered with DateOrdered with DoubleOrdered with Counter {
  def getList(ids: Seq[UUID], user: User = null) = {

    var userIds: Set[UUID] = Set()
    // get answers first
    val rows = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.COMMENT)
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

    val actions = if(user != null) UserAction.get(user.id, "comment", ids.toSet) else Map[UUID, Map[String, String]]()

    rows.map { row =>
      (
        Map(
          "id" -> row.getUUID("id"),
          "creator" -> users(row.getUUID("creator_id")),
          "body" -> row.getString("body"),
          "updated" -> row.getDate("updated"),
          "voteUp" -> row.getLong("vote_up"),
          "voteDown" -> row.getLong("vote_down"),
          "confidence" -> row.getDouble("confidence"),
          "commentCount" -> row.getLong("comment_count"),
          "actions" -> actions.get(row.getUUID("id")).getOrElse(Map())
        )
        )
    }
  }

  def getBest(key: String, size: Int, next: (Double, UUID), user: User = null) = {
    val data = getDoubleIds(Table.COMMENT, "confidence", key, size, next, true)

    getList(data.map(_._1).toSeq, user)
  }

  def create(body: String, target: String, target_id: UUID, creator: User)(implicit request: RequestHeader) = {
    // single field
    val id = UUIDs.timeBased()
    val created = new Date()
    val updated = new Date()
    val confidence = utils.Score.confidence(0, 0)

    Cassandra.session.execute(
      QueryBuilder.insertInto(Table.COMMENT)
        .value("id", id)
        .value("creator_id", creator.id)
        .value("parent_id", target_id)
        .value("body", body)
        .value("created", created)
        .value("updated", updated)
        .value("status", 1)
        .value("vote_up", 0L)
        .value("vote_down", 0L)
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
      "confidence" -> confidence,
      "commentCount" -> 0L,
      "actions" -> Map()
    )

    PubSubHelper.publish(TOPIC_COMMENT, CommentCreateMessage(id, body, creator, created, updated, confidence, target, target_id))

    result
  }

  def getRow(input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.COMMENT)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }
}
