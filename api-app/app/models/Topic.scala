package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.querybuilder.QueryBuilder

import database.{ElasticSearch, Cassandra}
import constants.Table
import constants.PubSub._
import pubsub.{AliasData, TopicCreateMessage, PubSubHelper}
import utils.Alias
import models.v2.StringUnique
import java.util

object Topic {
  def create(name: String, creator: User) = {
    // get exist topic first
    val topicIdOpt = StringUnique.getTargetId(Table.TOPIC, "name", name)
    if(topicIdOpt.isDefined) {
      (topicIdOpt.get, name, false)
    }
    else {
      val id = UUIDs.timeBased()
      var status: java.lang.Boolean = true
      val created = new Date()
      val updated = new Date()
      val follower_count: java.lang.Long = 0
      val question_count: java.lang.Long = 0
      val alias = Alias.convert(name, Alias.TOPIC_KEY)

      StringUnique.create(StringUnique(Table.TOPIC + "_name", name, id))
      //    if(creator.permissions.contains("publish-topic")) status = true
      Cassandra.session.execute(
        QueryBuilder.insertInto(Table.TOPIC)
          .value("id", id)
          .value("status", status)
          .value("name", name)
          .value("alias", alias)
          .value("creator_id", creator.id)
          .value("created", created)
          .value("updated", updated)
          .value("follower_count", follower_count)
          .value("question_count", question_count)
      )
      PubSubHelper.publish(TOPIC_ALIAS, AliasData(Alias.TOPIC_KEY, alias, Map("id" -> id, "name" -> name)))
      PubSubHelper.publish(TOPIC_TOPIC, TopicCreateMessage(id, name, creator, created, updated, status))
      (id, name, true)
    }
  }

  def search(name: String, excludedIds: Set[UUID]) = {
    // get in cassandra first
    var exeIds = excludedIds
    var found: Boolean = false
    var ids: List[UUID] = List()
    StringUnique.getTargetId(Table.TOPIC, "name", name).map { p =>
      exeIds = exeIds ++ Set(p)
      ids = ids ::: List(p)
      found = true
    }
//    val hits = ElasticSearch.search(constants.ElasticSearch.INDEX_TYPE_TOPIC, Seq("name"), name, "name", 10, 0)
//    val list = hits.map { hit =>
//      (hit.getId, hit.getSource.toMap + ("highlight" -> hit.getHighlightFields.get("name").getFragments.toList.head.string())
//    }

    val hits = ElasticSearch.search(constants.ElasticSearch.INDEX_TYPE_TOPIC, Seq("name"), name, "name", 10, 0, exeIds.map(_.toString).toSeq)
    val listSearch = hits.map { hit =>
      ids = ids ::: List(UUID.fromString(hit.getId))
      (UUID.fromString(hit.getId), hit)
    }.toMap

    //select topics from ids by access to cassandra
    val topics = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.TOPIC)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all().map { row =>
      val id = row.getUUID("id")
      Map(
        "id" -> id,
        "highlight" -> listSearch.get(id).map(_.getHighlightFields.get("name").getFragments.toList.head.string()).getOrElse(name),
        "name" -> row.getString("name"),
        "followerCount" -> row.getLong("follower_count"),
        "questionCount" -> row.getLong("question_count")
      )
    }

    (topics, found)
  }

  def getRow(input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.TOPIC)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }

  def getRows(ids: Set[UUID]) = {
    Cassandra.session.execute(
      QueryBuilder.select().all().from(Table.TOPIC)
        .where(QueryBuilder.in("id", ids.toSeq:_*))
    ).all()
  }
}
