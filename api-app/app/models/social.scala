package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder

import database.Cassandra
import constants.Table
import pubsub.{UnFollowMessage, FollowMessage, PubSubHelper}
import com.datastax.driver.core.Row

trait Counter {
  val counter_table: String = "counter"

  def incr(orgTable: String, field: String, id: UUID) = {
    val key = orgTable + "_" + field
    Cassandra.session.execute(
      QueryBuilder.update(counter_table)
        .`with`(QueryBuilder.incr("value"))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    )

    // get vote-up and vote-down back to return
    val row = Cassandra.session.execute(
      QueryBuilder.select("value")
        .from(counter_table)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    ).one()

    val value = row.getLong("value")
    Cassandra.session.execute(
      QueryBuilder.update(orgTable)
        .`with`(QueryBuilder.set(field, value))
        .where(QueryBuilder.eq("id", id))
    )

    value
  }

  def incr(orgTable: String, field: String, id: UUID, step: java.lang.Long) = {
    val key = orgTable + "_" + field
    Cassandra.session.execute(
      QueryBuilder.update(counter_table)
        .`with`(QueryBuilder.incr("value", step))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    )

    // get vote-up and vote-down back to return
    val row = Cassandra.session.execute(
      QueryBuilder.select("value")
        .from(counter_table)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    ).one()

    val value = row.getLong("value")
    Cassandra.session.execute(
      QueryBuilder.update(orgTable)
        .`with`(QueryBuilder.set(field, value))
        .where(QueryBuilder.eq("id", id))
    )

    value
  }

  def decr(orgTable: String, field: String, id: UUID) = {
    val key = orgTable + "_" + field
    Cassandra.session.execute(
      QueryBuilder.update(counter_table)
        .`with`(QueryBuilder.decr("value"))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    )

    // get vote-up and vote-down back to return
    val row = Cassandra.session.execute(
      QueryBuilder.select("value")
        .from(counter_table)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    ).one()

    val value = row.getLong("value")
    Cassandra.session.execute(
      QueryBuilder.update(orgTable)
        .`with`(QueryBuilder.set(field, value))
        .where(QueryBuilder.eq("id", id))
    )

    value
  }

  def decr(orgTable: String, field: String, id: UUID, step: Long) = {
    val key = orgTable + "_" + field
    Cassandra.session.execute(
      QueryBuilder.update(counter_table)
        .`with`(QueryBuilder.decr("value", step))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    )

    // get vote-up and vote-down back to return
    val row = Cassandra.session.execute(
      QueryBuilder.select("value")
        .from(counter_table)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    ).one()

    val value = row.getLong("value")
    Cassandra.session.execute(
      QueryBuilder.update(orgTable)
        .`with`(QueryBuilder.set(field, value))
        .where(QueryBuilder.eq("id", id))
    )

    value
  }

  def getCounter(orgTable: String, field: String, id: UUID) = {
    val key = orgTable + "_" + field
    // get field
    val row = Cassandra.session.execute(
      QueryBuilder.select(field)
        .from(counter_table)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
    ).one()

    row.getLong("value")
  }
}

trait Follow {
  val followTable = "follow"

  def followTarget(userId: UUID, target: String, target_id: UUID, about: String = "follow") = {
    val updated = new Date()
    val key = target + "_" + target_id.toString
    // get current status of follow
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(followTable)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("follower_id", userId))
    ).one()

    if(row != null) false
    else {
      Cassandra.session.execute(
        QueryBuilder.insertInto(followTable)
          .value("key", key)
          .value("follower_id", userId)
          .value("updated", updated)
      )

      PubSubHelper.publish(constants.PubSub.TOPIC_FOLLOW, FollowMessage(userId, target, target_id, updated, about))
      true
    }
  }

  def unFollowTarget(userId: UUID, target: String, target_id: UUID) = {
    val key = target + "_" + target_id.toString
    // get current status of follow
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(followTable)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("follower_id", userId))
    ).one()

    if(row == null) false
    else {
      Cassandra.session.execute(
        QueryBuilder.delete().from(followTable)
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("follower_id", userId))
      )

      PubSubHelper.publish(constants.PubSub.TOPIC_FOLLOW, UnFollowMessage(userId, target, target_id))
      true
    }
  }

  def getFollowers(target: String, targetId: UUID) = {
    val key = target + "_" + targetId.toString
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(followTable)
        .where(QueryBuilder.eq("key", key))
    ).all().map(_.getUUID("follower_id"))
  }

  def getFollowTarget(userId: UUID) = {
    val follows: scala.collection.mutable.Map[String, Set[UUID]] = scala.collection.mutable.Map()

    Cassandra.session.execute(
      QueryBuilder.select().all().from(Table.USER_FOLLOW)
        .where(QueryBuilder.eq("user_id", userId))
    ).all().foreach { row =>
      val target =  row.getString("target")
      if(!follows.isDefinedAt(target)) follows(target) = Set[UUID]()

      follows(target) = follows(target) + row.getUUID("target_id")
    }

    follows
  }

  def getFollowTarget(userId: UUID, target: String) = {
    Cassandra.session.execute(
      QueryBuilder.select().all().from(Table.USER_FOLLOW)
        .where(QueryBuilder.eq("user_id", userId))
        .and(QueryBuilder.eq("target", target))
    ).all().map { row =>
      row.getUUID("target_id")
    }
  }

  def getFollowTarget(userIds: Set[UUID]) = {
    val follows: scala.collection.mutable.Map[UUID, scala.collection.mutable.Map[String, Set[UUID]]] = scala.collection.mutable.Map()

    Cassandra.session.execute(
      QueryBuilder.select().all().from(Table.USER_FOLLOW)
        .where(QueryBuilder.in("user_id", userIds.toSeq:_*))
    ).all().foreach { row =>
      val userId = row.getUUID("user_id")
      val target =  row.getString("target")
      if(!follows.isDefinedAt(userId)) follows(userId) = scala.collection.mutable.Map[String, Set[UUID]]()

      if(!follows(userId).isDefinedAt(target)) follows(userId)(target) = Set[UUID]()

      follows(userId)(target) = follows(userId)(target) + row.getUUID("target_id")
    }

    follows
  }
}


trait LongOrdered {
  val longOrderedTable: String = "long_ordered"
  val longOrderedTrackTable: String = "long_ordered_track"

  def getLongIds(table: String, field: String, prefix: String = null, limit: Int, next: (Long, UUID) = null, desc: Boolean = true) = {
    val key = if(prefix != null) table + "_" + field + "_" + prefix  else table + "_" + field

    val order = if(desc) QueryBuilder.desc("value") else QueryBuilder.asc("value")
    var query = QueryBuilder.select().all()
      .from(longOrderedTable)
      .where(QueryBuilder.eq("key", key))

    if(next != null) {
      if(!desc) query = query.and(QueryBuilder.gt(List("value", "id"), List[AnyRef]( next._1: java.lang.Long, next._2)))
      else query = query.and(QueryBuilder.lt(List("value", "id"), List[AnyRef]( next._1: java.lang.Long, next._2)))
    }

    Cassandra.session.execute(
      query.limit(limit).orderBy(order)
    )
    .all().map(p => (p.getUUID("id"), (p.getDate("updated"), p.getString("action"), p.getString("f"), p.getUUID("fid"), p.getUUID("answer_id"))))
  }

  def longUpdate(table: String, field: String, id: UUID, value: java.lang.Long, prefixes: Set[String], updated: Date, action: String = null, from: String = null, fromId: UUID = null, answerId: UUID = null) = {
    val keys = if(prefixes.size > 0) prefixes.map(table + "_" + field + "_" + _) else Set(table + "_" + field)

    keys.foreach { key =>
      var r_action = action
      var r_from = from
      var r_fromId = fromId
      var r_answerId = answerId

      //find in score track to make sure delete old comment in comment score
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(longOrderedTrackTable)
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("id", id))
      ).one()

      val oldValue = if(longRow == null) 0L else longRow.getLong("value")

      if(longRow != null) {
        if(action == null) r_action = longRow.getString("action")
        if(from == null) r_from = longRow.getString("f")
        if(fromId == null) r_fromId = longRow.getUUID("fid")
        if(answerId == null) r_answerId = longRow.getUUID("answer_id")
        // delete old score
        Cassandra.session.execute(
          QueryBuilder.delete()
            .all()
            .from(longOrderedTable)
            .where(QueryBuilder.eq("key", key))
            .and(QueryBuilder.eq("value", oldValue))
            .and(QueryBuilder.eq("id", id))
        )
      }

      // update new score for this comment
      Cassandra.session.execute(
        QueryBuilder.update(longOrderedTrackTable)
        .`with`(QueryBuilder.set("value", value))
        .and(QueryBuilder.set("action", r_action))
        .and(QueryBuilder.set("f", r_from))
        .and(QueryBuilder.set("fid", r_fromId))
        .and(QueryBuilder.set("answer_id", r_answerId))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("id", id))
      )

      //update new score for comment score
      Cassandra.session.execute(
        QueryBuilder.insertInto(longOrderedTable)
          .value("key", key)
          .value("value", value)
          .value("id", id)
          .value("updated", updated)
          .value("action", r_action)
          .value("f", r_from)
          .value("fid", r_fromId)
          .value("answer_id", r_answerId)
      )
    }
  }

  def longCopy(fromKey: String, toKey: String, first: Int = 50) = {
    var r = 0
    var batch = QueryBuilder.batch()
    var lastRow: Row = null
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(longOrderedTable)
        .where(QueryBuilder.eq("key", fromKey))
        .orderBy(QueryBuilder.desc("value"))
        .limit(first)
    ).all().map { row =>
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(longOrderedTrackTable)
          .where(QueryBuilder.eq("key", toKey))
          .and(QueryBuilder.eq("id", row.getUUID("id")))
      ).one()

      if(longRow != null) {
        // delete old score
        batch.add(
          QueryBuilder.delete()
            .all()
            .from(longOrderedTable)
            .where(QueryBuilder.eq("key", fromKey))
            .and(QueryBuilder.eq("value", longRow.getLong("value")))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        )
      }
      // update longOrderedTable & longOrderedTrackTable for first 50 items
      batch.add(
        QueryBuilder.insertInto(longOrderedTable)
          .value("key", toKey)
          .value("value", row.getLong("value"))
          .value("id", row.getUUID("id"))
          .value("updated", row.getDate("updated"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
      )

      batch.add(
        QueryBuilder.insertInto(longOrderedTrackTable)
          .value("value", row.getLong("value"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
          .value("key", toKey)
          .value("id", row.getUUID("id"))
      )

      r = r + 1
      lastRow = row
    }

    Cassandra.session.execute(batch)
    if(r >= first) {
      batch = QueryBuilder.batch()
      Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(longOrderedTable)
          .where(QueryBuilder.eq("key", fromKey))
          .and(QueryBuilder.lt(List("value", "id"), List[AnyRef](lastRow.getLong("value"): java.lang.Long, lastRow.getUUID("id"))))
          .orderBy(QueryBuilder.desc("value"))
      ).all().map { row =>
        val longRow = Cassandra.session.execute(
          QueryBuilder.select().all()
            .from(longOrderedTrackTable)
            .where(QueryBuilder.eq("key", toKey))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        ).one()

        if(longRow != null) {
          // delete old score
          batch.add(
            QueryBuilder.delete()
              .all()
              .from(longOrderedTable)
              .where(QueryBuilder.eq("key", fromKey))
              .and(QueryBuilder.eq("value", longRow.getLong("value")))
              .and(QueryBuilder.eq("id", row.getUUID("id")))
          )
        }
      // update longOrderedTable & longOrderedTrackTable for first 50 items
        batch.add(
          QueryBuilder.insertInto(longOrderedTable)
            .value("key", toKey)
            .value("value", row.getLong("value"))
            .value("id", row.getUUID("id"))
            .value("updated", row.getDate("updated"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
        )

        batch.add(
          QueryBuilder.insertInto(longOrderedTrackTable)
            .value("value", row.getLong("value"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
            .value("key", toKey)
            .value("id", row.getUUID("id"))
        )

        r = r + 1
      }

      Cassandra.session.executeAsync(batch)
    }
  }
}


trait DateOrdered {
  val dateOrderedTable: String = "date_ordered"
  val dateOrderedTrackTable: String = "date_ordered_track"

  def getDateIds(table: String, field: String, prefix: String = null, limit: Int, next: (Long, UUID) = null, desc: Boolean = true) = {
    val key = if(prefix != null) table + "_" + field + "_" + prefix else table + "_" + field

    val order = if(desc) QueryBuilder.desc("value") else QueryBuilder.asc("value")
    var query = QueryBuilder.select().all()
      .from(dateOrderedTable)
      .where(QueryBuilder.eq("key", key))

    if(next != null) {
      if(!desc) query = query.and(QueryBuilder.gt(List("value", "id"), List[AnyRef]( next._1: java.lang.Long, next._2)))
      else query = query.and(QueryBuilder.lt(List("value", "id"), List[AnyRef]( next._1: java.lang.Long, next._2)))
    }

    Cassandra.session.execute(
      query.limit(limit).orderBy(order)
    )
      .all().map(p => (p.getUUID("id"), (p.getDate("updated"), p.getString("action"), p.getString("f"), p.getUUID("fid"), p.getUUID("answer_id"))))
  }

  def dateUpdate(table: String, field: String, id: UUID, value: Date, prefixes: Set[String], updated: Date, action: String = null, from: String = null, fromId: UUID = null, answerId: UUID = null) = {
    val keys = if(prefixes.size > 0) prefixes.map(table + "_" + field + "_" + _) else Set(table + "_" + field)

    keys.foreach { key =>
      var r_action = action
      var r_from = from
      var r_fromId = fromId
      var r_answerId = answerId
    //find in score track to make sure delete old comment in comment score
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(dateOrderedTrackTable)
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("id", id))
      ).one()

      val oldValue = if(longRow == null) null else longRow.getDate("value")

      if(longRow != null) {
        if(action == null) r_action = longRow.getString("action")
        if(from == null) r_from = longRow.getString("f")
        if(fromId == null) r_fromId = longRow.getUUID("fid")
        if(answerId == null) r_answerId = longRow.getUUID("answer_id")
        // delete old score
        Cassandra.session.execute(
          QueryBuilder.delete()
            .all()
            .from(dateOrderedTable)
            .where(QueryBuilder.eq("key", key))
            .and(QueryBuilder.eq("value", oldValue))
            .and(QueryBuilder.eq("id", id))
        )
      }

      // update new score for this comment
      Cassandra.session.execute(
        QueryBuilder.update(dateOrderedTrackTable)
          .`with`(QueryBuilder.set("value", value))
          .and(QueryBuilder.set("action", r_action))
          .and(QueryBuilder.set("f", r_from))
          .and(QueryBuilder.set("fid", r_fromId))
          .and(QueryBuilder.set("answer_id", r_answerId))
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("id", id))
      )

      //update new score for comment score
      Cassandra.session.execute(
        QueryBuilder.insertInto(dateOrderedTable)
          .value("key", key)
          .value("value", value)
          .value("id", id)
          .value("updated", updated)
          .value("action", r_action)
          .value("f", r_from)
          .value("fid", r_fromId)
          .value("answer_id", r_answerId)
      )
    }
  }

  def dateCopy(fromKey: String, toKey: String, first: Int = 50) = {
    var r = 0
    var batch = QueryBuilder.batch()
    var lastRow: Row = null
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(dateOrderedTable)
        .where(QueryBuilder.eq("key", fromKey))
        .orderBy(QueryBuilder.desc("value"))
        .limit(first)
    ).all().map { row =>
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(dateOrderedTrackTable)
          .where(QueryBuilder.eq("key", toKey))
          .and(QueryBuilder.eq("id", row.getUUID("id")))
      ).one()

      if(longRow != null) {
        // delete old score
        batch.add(
          QueryBuilder.delete()
            .all()
            .from(dateOrderedTable)
            .where(QueryBuilder.eq("key", toKey))
            .and(QueryBuilder.eq("value", longRow.getDate("value")))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        )
      }
    // update longOrderedTable & longOrderedTrackTable for first 50 items
      batch.add(
        QueryBuilder.insertInto(dateOrderedTable)
          .value("key", toKey)
          .value("value", row.getDate("value"))
          .value("id", row.getUUID("id"))
          .value("updated", row.getDate("updated"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
      )

      batch.add(
        QueryBuilder.insertInto(dateOrderedTrackTable)
          .value("value", row.getDate("value"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
          .value("key", toKey)
          .value("id", row.getUUID("id"))
      )

      r = r + 1
      lastRow = row
    }

    Cassandra.session.execute(batch)
    if(r >= first) {
      batch = QueryBuilder.batch()
      Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(dateOrderedTable)
          .where(QueryBuilder.eq("key", fromKey))
          .and(QueryBuilder.lt(List("value", "id"), List[AnyRef](lastRow.getDate("value").getTime: java.lang.Long, lastRow.getUUID("id"))))
          .orderBy(QueryBuilder.desc("value"))
      ).all().map { row =>
        val longRow = Cassandra.session.execute(
          QueryBuilder.select().all()
            .from(dateOrderedTrackTable)
            .where(QueryBuilder.eq("key", toKey))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        ).one()

        if(longRow != null) {
          // delete old score
          batch.add(
            QueryBuilder.delete()
              .all()
              .from(dateOrderedTable)
              .where(QueryBuilder.eq("key", toKey))
              .and(QueryBuilder.eq("value", longRow.getDate("value")))
              .and(QueryBuilder.eq("id", row.getUUID("id")))
          )
        }
      // update longOrderedTable & longOrderedTrackTable for first 50 items
        batch.add(
          QueryBuilder.insertInto(dateOrderedTable)
            .value("key", toKey)
            .value("value", row.getDate("value"))
            .value("id", row.getUUID("id"))
            .value("updated", row.getDate("updated"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
        )

        batch.add(
          QueryBuilder.insertInto(dateOrderedTrackTable)
            .value("value", row.getDate("value"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
            .value("key", toKey)
            .value("id", row.getUUID("id"))
        )

        r = r + 1
      }

      Cassandra.session.executeAsync(batch)
    }
  }
}

trait DoubleOrdered {
  val doubleOrderedTable: String = "double_ordered"
  val doubleOrderedTrackTable: String = "double_ordered_track"

  def getDoubleIds(table: String, field: String, prefix: String = null, limit: Int, next: (Double, UUID) = null, desc: Boolean = true) = {
    val key = if(prefix != null) table + "_" + field + "_" + prefix else table + "_" + field

    val order = if(desc) QueryBuilder.desc("value") else QueryBuilder.asc("value")
    var query = QueryBuilder.select().all()
      .from(doubleOrderedTable)
      .where(QueryBuilder.eq("key", key))

    if(next != null) {
      if(!desc) query = query.and(QueryBuilder.gt(List("value", "id"), List[AnyRef]( next._1: java.lang.Double, next._2)))
      else query = query.and(QueryBuilder.lt(List("value", "id"), List[AnyRef]( next._1: java.lang.Double, next._2)))
    }

    Cassandra.session.execute(
      query.limit(limit).orderBy(order)
    )
      .all().map(p => (p.getUUID("id"), (p.getDate("updated"), p.getString("action"), p.getString("f"), p.getUUID("fid"), p.getUUID("answer_id"))))
  }

  def doubleUpdate(table: String, field: String, id: UUID, value: Double, prefixes: Set[String], updated: Date, action: String = null, from: String = null, fromId: UUID = null, answerId: UUID = null) = {
    val keys = if(prefixes.size > 0) prefixes.map(table + "_" + field + "_" + _) else Set(table + "_" + field)

    keys.foreach { key =>
      var r_action = action
      var r_from = from
      var r_fromId = fromId
      var r_answerId = answerId
    //find in score track to make sure delete old comment in comment score
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(doubleOrderedTrackTable)
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("id", id))
      ).one()

      val oldValue = if(longRow == null) 0D else longRow.getDouble("value")

      if(longRow != null) {
        if(action == null) r_action = longRow.getString("action")
        if(from == null) r_from = longRow.getString("f")
        if(fromId == null) r_fromId = longRow.getUUID("fid")
        if(answerId == null) r_answerId = longRow.getUUID("answer_id")
        // delete old score
        Cassandra.session.execute(
          QueryBuilder.delete()
            .all()
            .from(doubleOrderedTable)
            .where(QueryBuilder.eq("key", key))
            .and(QueryBuilder.eq("value", oldValue))
            .and(QueryBuilder.eq("id", id))
        )
      }

      // update new score for this comment
      Cassandra.session.execute(
        QueryBuilder.update(doubleOrderedTrackTable)
          .`with`(QueryBuilder.set("value", value))
          .and(QueryBuilder.set("action", r_action))
          .and(QueryBuilder.set("f", r_from))
          .and(QueryBuilder.set("fid", r_fromId))
          .and(QueryBuilder.set("answer_id", r_answerId))
          .where(QueryBuilder.eq("key", key))
          .and(QueryBuilder.eq("id", id))
      )

      //update new score for comment score
      Cassandra.session.execute(
        QueryBuilder.insertInto(doubleOrderedTable)
          .value("key", key)
          .value("value", value)
          .value("id", id)
          .value("updated", updated)
          .value("action", r_action)
          .value("f", r_from)
          .value("fid", r_fromId)
          .value("answer_id", r_answerId)
      )
    }
  }

  def doubleCopy(fromKey: String, toKey: String, first: Int = 50) = {
    var r = 0
    var batch = QueryBuilder.batch()
    var lastRow: Row = null
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(doubleOrderedTable)
        .where(QueryBuilder.eq("key", fromKey))
        .orderBy(QueryBuilder.desc("value"))
        .limit(first)
    ).all().map { row =>
      val longRow = Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(doubleOrderedTrackTable)
          .where(QueryBuilder.eq("key", toKey))
          .and(QueryBuilder.eq("id", row.getUUID("id")))
      ).one()


      if(longRow != null) {
        // delete old score
        batch.add(
          QueryBuilder.delete()
            .all()
            .from(doubleOrderedTable)
            .where(QueryBuilder.eq("key", toKey))
            .and(QueryBuilder.eq("value", longRow.getDouble("value")))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        )
      }
    // update longOrderedTable & longOrderedTrackTable for first 50 items
      batch.add(
        QueryBuilder.insertInto(doubleOrderedTable)
          .value("key", toKey)
          .value("value", row.getDouble("value"))
          .value("id", row.getUUID("id"))
          .value("updated", row.getDate("updated"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
      )

      batch.add(
        QueryBuilder.insertInto(doubleOrderedTrackTable)
          .value("value", row.getDouble("value"))
          .value("action", row.getString("action"))
          .value("f", row.getString("f"))
          .value("fid", row.getUUID("fid"))
          .value("answer_id", row.getUUID("answer_id"))
          .value("key", toKey)
          .value("id", row.getUUID("id"))
      )

      r = r + 1
      lastRow = row
    }

    Cassandra.session.execute(batch)
    if(r >= first) {
      batch = QueryBuilder.batch()
      Cassandra.session.execute(
        QueryBuilder.select().all()
          .from(doubleOrderedTable)
          .where(QueryBuilder.eq("key", fromKey))
          .and(QueryBuilder.lt(List("value", "id"), List[AnyRef](lastRow.getDouble("value"): java.lang.Double, lastRow.getUUID("id"))))
          .orderBy(QueryBuilder.desc("value"))
      ).all().map { row =>
        val longRow = Cassandra.session.execute(
          QueryBuilder.select().all()
            .from(doubleOrderedTrackTable)
            .where(QueryBuilder.eq("key", toKey))
            .and(QueryBuilder.eq("id", row.getUUID("id")))
        ).one()


        if(longRow != null) {
          // delete old score
          batch.add(
            QueryBuilder.delete()
              .all()
              .from(doubleOrderedTable)
              .where(QueryBuilder.eq("key", toKey))
              .and(QueryBuilder.eq("value", longRow.getDouble("value")))
              .and(QueryBuilder.eq("id", row.getUUID("id")))
          )
        }
      // update longOrderedTable & longOrderedTrackTable for first 50 items
        batch.add(
          QueryBuilder.insertInto(doubleOrderedTable)
            .value("key", toKey)
            .value("value", row.getDouble("value"))
            .value("id", row.getUUID("id"))
            .value("updated", row.getDate("updated"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
        )

        batch.add(
          QueryBuilder.insertInto(doubleOrderedTrackTable)
            .value("value", row.getDouble("value"))
            .value("action", row.getString("action"))
            .value("f", row.getString("f"))
            .value("fid", row.getUUID("fid"))
            .value("answer_id", row.getUUID("answer_id"))
            .value("key", toKey)
            .value("id", row.getUUID("id"))
        )

        r = r + 1
      }

      Cassandra.session.executeAsync(batch)
    }
  }

  def getDoubleTopRow(table: String, field: String, key: String) = {
    val row = Cassandra.session.execute(
      QueryBuilder.select().all.from(doubleOrderedTable)
        .where(QueryBuilder.eq("key", table + "_" + field + "_" + key))
        .orderBy(QueryBuilder.desc("value"))
        .limit(1)
    ).one()

    row
  }
}