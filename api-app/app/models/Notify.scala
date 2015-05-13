package models

import java.util.{UUID, Date}
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.utils.UUIDs

import constants.PubSub._
import constants.Content._
import pubsub._
import database.Cassandra
import utils.StringHelper
import scala.Some
import constants.Table
import security.oauth2.TokenStorage
import restapis.UserRest
import play.api.mvc.RequestHeader


object Notify extends Follow with DateOrdered with Counter {
  def _getNotifications(ids: Seq[UUID]) = {
    var cql: String = ""

    cql = QueryBuilder.select()
      .all()
      .from(Table.NOTIFY)
      .where(QueryBuilder.in("id", ids:_*))
      .toString

    val rows = Cassandra.session.execute(cql).all()

    var userIds: Set[UUID] = Set()

    rows.foreach{ row =>
      userIds ++= row.getMap("users", classOf[java.util.UUID], classOf[java.lang.Long]).keySet().toSet
    }

    // need to get user info here
    val users = Cassandra.session.execute(
      QueryBuilder.select()
        .all()
        .from(Table.USER)
        .where(QueryBuilder.in("id", userIds.toSeq:_*))
    ).all().map { row =>
      val id = row.getUUID("id")
      val data = Map(
        "id" -> id,
        "display" -> row.getString("display"),
        "alias" -> row.getString("alias")
      )

      (id, data)
    }.toMap

    rows.map { row =>
      val u = row.getMap("users", classOf[java.util.UUID], classOf[java.lang.Long]).toList.sortBy(_._2).map { userInfo =>
        users.getOrElse(userInfo._1, Map())
      }
      Map(
        "id" -> row.getUUID("id"),
        "users" -> u,
        "type" -> row.getString("type"),
        "target_id" -> row.getUUID("target_id"),
        "title" -> row.getString("title"),
        "link" -> row.getString("link"),
        "action" -> row.getString("action"),
        "updated" -> row.getDate("updated"),
        "status" -> row.getBool("status")
      )
    }
  }

  def addNotify(action: String, target_type: String, target_id: UUID, owner: UUID, title: String, link: String) = {
    val followers = getFollowers(target_type, target_id)

    // get unread notification first by notify_track table
    val key = action + "_" + target_type + "_" + target_id.toString
    val unreads = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.NOTIFY_TRACK)
        .where(QueryBuilder.eq("key", key))
    ).all().map { row =>
      (row.getUUID("user_id"), row.getUUID("notify_id"))
    }.toMap

    val id = UUIDs.timeBased()

    val batch = QueryBuilder.batch()
    followers.foreach { followerId =>
      val date = new Date()
      if(followerId == owner) {

      }
      else if(unreads.isDefinedAt(followerId)) {
        val notifyId = unreads(followerId)
        batch.add(
          QueryBuilder.update(Table.NOTIFY)
            .where(QueryBuilder.eq("id", notifyId))
            .`with`(QueryBuilder.put("users", owner, date.getTime))
            .and(QueryBuilder.set("updated", date))
        )
        dateUpdate(Table.NOTIFY, "updated", notifyId, date, Set(followerId.toString), date)
      }
      else {
        batch.add(
          QueryBuilder.insertInto(Table.NOTIFY)
            .value("id", id)
            .value("user_id", id)
            .value("type", target_type)
            .value("target_id", target_id)
            .value("title", title)
            .value("link", link)
            .value("action", action)
            .value("users", new java.util.TreeMap[UUID, java.lang.Long](){ put(owner, date.getTime)  })
            .value("updated", date)
            .value("status", Boolean.box(false))
        )

        batch.add(
          QueryBuilder.insertInto(Table.NOTIFY_TRACK)
            .value("key", key)
            .value("user_id", followerId)
            .value("notify_id", id)
        )

        incr(Table.USER, "unread", followerId)
        dateUpdate(Table.NOTIFY, "updated", id, date, Set(followerId.toString), date)
        TokenStorage.addNotify(followerId)
      }
    }

    Cassandra.session.execute(batch)
  }

  def resetNotify(userId: UUID, keys: Set[String], limit: Int) = {
    Cassandra.session.execute(
      QueryBuilder.delete()
        .all()
        .from(Table.NOTIFY_TRACK)
        .where(QueryBuilder.in("key", keys.toSeq:_*))
    )

    Cassandra.session.execute(
      QueryBuilder.update(Table.USER)
      .`with`(
        QueryBuilder.set("unread", Long.box(0L))
      ).where(QueryBuilder.eq("id", userId))
    )
  }

  def getNotifications(userId: UUID, unreadNumber: Long) = {
    val MIN_NOTIFICATION = 10
    val limit = if(unreadNumber > MIN_NOTIFICATION) unreadNumber else MIN_NOTIFICATION

    val notifyIds = getDateIds(Table.NOTIFY, "updated", userId.toString, limit.toInt)

    val notifications = _getNotifications(notifyIds.map(_._1))
    val notifyTrackKeys = notifications.map(p => p("action").toString + "_" + p("type").toString + "_" + p("target_id").toString)
    PubSubHelper.publish(TOPIC_NOTIFY, GetNotifyData(userId, notifyTrackKeys.toSet, limit.toInt))

    notifications
  }
}