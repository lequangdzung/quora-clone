package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.querybuilder.QueryBuilder
import play.api.mvc.RequestHeader

import database.{ElasticSearch, Cassandra}
import constants.{StaticNumber, Table}
import constants.PubSub._
import pubsub.{AliasData, QuestionMessage, PubSubHelper}
import utils.Alias

object Question extends LongOrdered with DateOrdered with DoubleOrdered with Follow {
  def getList(ids: Seq[UUID], data: Map[UUID, (Date, String, String, UUID, UUID)], user: User = null, isBest: Boolean = false) = {

    val rows = Cassandra.session.execute(
      QueryBuilder.select()
        .all()
        .from(Table.QUESTION)
        .where(QueryBuilder.in("id", ids:_*))
    ).all()

    // get promote answer first
    var userIds: Set[UUID] = data.flatMap { p =>
      if(p._2._3 != null && p._2._3.equals("user")) {
        Some(p._2._4)
      }
      else None
    }.toSet

    val topicIds: Set[UUID] = data.flatMap { p =>
      if(p._2._3 != null && p._2._3.equals("topic")) {
        Some(p._2._4)
      }
      else None
    }.toSet

    val answer_ids: Set[UUID] = data.flatMap { p =>
      if(p._2._5 != null) {
        Some(p._2._5)
      }
      else None
    }.toSet

    // get answers first
    val answer_rows = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.ANSWER)
        .where(QueryBuilder.in("id", answer_ids.toSeq:_*))
    ).all()

    answer_rows.foreach { row =>
      userIds += row.getUUID("creator_id")
    }


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
          "display" -> row.getString("display")
        )
      )
    }.toMap

    val answerActions = if(user != null) UserAction.get(user.id, "answer", answer_ids.toSet) else Map[UUID, Map[String, String]]()
    val answers = answer_rows.map { row =>
      (
        row.getUUID("id"),
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
          "commentCount" -> row.getLong("comment_count"),
          "followerCount" -> row.getLong("follower_count"),
          "actions" -> answerActions.get(row.getUUID("id")).getOrElse(Map())
        )
      )
    }.toMap

    val topics = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.TOPIC)
        .where(QueryBuilder.in("id", topicIds.toSeq:_*))
    ).all().flatMap { row =>
      val id = row.getUUID("id")
      if(row.getBool("status").equals(true) || (user.id != null && (row.getUUID("creator_id") == user.id)))
        List(
          (
            id,
            Map(
              "id" -> id,
              "title" -> row.getString("name"),
              "alias" -> row.getString("alias"),
              "status" -> row.getBool("status"),
              "followerCount" -> row.getLong("follower_count")
            )
          )
        )
      else
        List()
    }.toMap

    val actions = if(user != null) UserAction.get(user.id, "question", ids.toSet) else Map[UUID, Map[String, String]]()

    rows.map { row =>
      val id = row.getUUID("id")
      val rowData = data(id)
      Map(
        "id" -> id,
        "creator_id" -> row.getUUID("creator_id"),
        "title" -> row.getString("title"),
        "alias" -> row.getString("alias"),
        "description" -> row.getString("description"),
        "shortDesc" -> row.getString("short_description"),
        "created" -> row.getDate("created"),
        "updated" -> row.getDate("updated"),
        "vote" -> row.getLong("vote"),
        "score" -> row.getLong("score"),
        "commentCount" -> row.getLong("comment_count"),
        "followerCount" -> row.getLong("follower_count"),
        "answerCount" -> row.getLong("answer_count"),
        "userCount" -> row.getLong("user_count"),
        "confidence" -> row.getDouble("confidence"),
        "data" -> Map(
          "updated" -> rowData._1,
          "action" -> rowData._2,
          "targetName" -> rowData._3,
          "target" -> {
            if(rowData._3 == "user" && rowData._4 != null) users(rowData._4)
            else if(rowData._3 == "topic" && rowData._4 != null) topics(rowData._4)
            else null
          },
          "answer" -> (if(rowData._5 != null) answers(rowData._5) else null)
        ),
        "actions" -> actions.get(row.getUUID("id")).getOrElse(Map())
      )
    }
  }

  def get(id: UUID, user: User = null) = {
    val row = Cassandra.session.execute(
      QueryBuilder.select()
        .all()
        .from(Table.QUESTION)
        .where(QueryBuilder.eq("id", id))
    ).one()

    // get promote answer first
    var userIds: Set[UUID] = Set()
    var topicIds: Set[UUID] = Set()

    userIds += row.getUUID("creator_id")
    topicIds = topicIds ++ row.getList("topics", classOf[UUID])

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
          "display" -> row.getString("display")
        )
        )
    }.toMap

    val topicActions = if(user != null) UserAction.get(user.id, "topic", topicIds.toSet) else Map[UUID, Map[String, String]]()
    val topics = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.TOPIC)
        .where(QueryBuilder.in("id", topicIds.toSeq:_*))
    ).all().flatMap { row =>
      val id = row.getUUID("id")
      if(row.getBool("status").equals(true) || (user != null && user.id != null && (row.getUUID("creator_id") == user.id)))
        List(
          (
            id,
            Map(
              "id" -> id,
              "name" -> row.getString("name"),
              "alias" -> row.getString("alias"),
              "status" -> row.getBool("status"),
              "followerCount" -> row.getLong("follower_count"),
              "questionCount" -> row.getLong("question_count"),
              "actions" -> topicActions.get(row.getUUID("id")).getOrElse(Map())
            )
            )
        )
      else
        List()
    }.toMap


    val actions = if(user != null) UserAction.get(user.id, "question", Set(id)) else Map[UUID, Map[String, String]]()

    Map(
      "id" -> row.getUUID("id"),
      "creator" -> users(row.getUUID("creator_id")),
      "title" -> row.getString("title"),
      "alias" -> row.getString("alias"),
      "description" -> row.getString("description"),
      "shortDesc" -> row.getString("short_description"),
      "topics" -> row.getList("topics", classOf[UUID]).flatMap(p => topics.get(p)).toList,
      "created" -> row.getDate("created"),
      "updated" -> row.getDate("updated"),
      "vote" -> row.getLong("vote"),
      "score" -> row.getLong("score"),
      "commentCount" -> row.getLong("comment_count"),
      "answerCount" -> row.getLong("answer_count"),
      "followerCount" -> row.getLong("follower_count"),
      "userCount" -> row.getLong("user_count"),
      "confidence" -> row.getDouble("confidence"),
      "actions" -> actions.get(row.getUUID("id")).getOrElse(Map()),
      "answers" -> Answer.getHot("question_" + id.toString, StaticNumber.ANSWER_PER_PAGE , null, user)
    )
  }

  def getHot(key: String, size: Int, next: (Long, UUID), user: User = null) = {
    val data = getLongIds(Table.QUESTION, "score", key, size, next, true)
    getList(data.map(_._1).toSeq, data.toMap, user)
  }

  def getNewest(key: String, size: Int, next: (Long, UUID), user: User = null) = {
    val data = getDateIds(Table.QUESTION, "updated", key, size, next, true)
    getList(data.map(_._1).toSeq, data.toMap, user)
  }

  def getBestAnswer(key: String, size: Int, next: (Double, UUID), user: User = null) = {
    val data = getDoubleIds(Table.QUESTION, "confidence", key, size, next, true)
    getList(data.map(_._1).toSeq, data.toMap, user, true)
  }

  def confirm(title: String) = {
    val hits = ElasticSearch.search(constants.ElasticSearch.INDEX_TYPE_QUESTION, Seq("title"), title, "title", 5, 0)
    hits.map { hit =>
      hit.getSource.toMap + ("title" -> hit.getHighlightFields.get("title").getFragments.toList.head.string())
    }
  }

  def search(query: String) = {
    val hits = ElasticSearch.search(constants.ElasticSearch.INDEX_TYPE_QUESTION, Seq("title", "body"), query, "title", 5, 0)
    hits.map { hit =>
      hit.getSource.toMap + ("title" -> hit.getHighlightFields.get("title").getFragments.toList.head.string())
    }
  }

  def create(title: String, description: String, short_desc: String, topics: List[Map[String, String]], creator: User)(implicit request: RequestHeader) = {
    // single field
    val id = UUIDs.timeBased()
    val created = new Date()
    val updated = new Date()
    val score = utils.Score.hot(1, created)

    // process tag first because we need to validate
    val topicIds = topics.flatMap { tag =>
      if(tag.isDefinedAt("id")) List(UUID.fromString(tag("id").toString))
      else List()
    }

    // get tags here
    val topicObjects = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.TOPIC)
        .where(QueryBuilder.in("id", topicIds.toSeq:_*))
    ).all().map{p => (p.getUUID("id"), p.getString("name"))}.toMap

//    if(topicObjects.isEmpty && !creator.permissions.contains("publish-topic")) {
//      throw exception.BadData("tag", "at least have one public tag")
//    }

    // try to get newTags from tags
    val newTopicIds = topics.flatMap { topic =>
      if(!topic.isDefinedAt("id") || !topicObjects.keySet.contains(UUID.fromString(topic("id")))) {
        val topicRet = Topic.create(topic("name"), creator)
        followTarget(creator.id, "topic", topicRet._1)
        List(topicRet._1)
      }
      else
        List()
    }

    val alias = Alias.convert(title, Alias.QUESTION_KEY)

    Cassandra.session.execute(
      QueryBuilder.insertInto(Table.QUESTION)
        .value("id", id)
        .value("creator_id", creator.id)
        .value("title", title)
        .value("alias", alias)
        .value("description", description)
        .value("short_description", short_desc)
        .value("topics", seqAsJavaList(topicObjects.keys.toSeq ++ newTopicIds.toSeq))
        .value("created", created)
        .value("updated", updated)
        .value("status", 1)
        .value("vote", 0L)
        .value("score", score)
        .value("comment_count", 0L)
        .value("follower_count", 1L)
        .value("user_count", 1L)
    )

    // after used this alias need to write to alias table
    PubSubHelper.publish(TOPIC_ALIAS, AliasData(Alias.QUESTION_KEY, alias, Map("id" -> id, "title" -> title)))

    val result = Map(
      "id" -> id,
      "creator" -> Map(
        "id" -> creator.id,
        "display" -> creator.display,
        "email" -> creator.email
      ),
      "title" -> title,
      "alias" -> alias,
      "score" -> score,
      "updated" -> updated,
      "created" -> created,
      "description" -> description,
      "shortDesc" -> short_desc,
      "commentCount" -> 0,
      "followerCount" -> 1,
      "vote" -> 0,
      "tags" -> topicObjects.map(p => Map("id" -> p._1, "title" -> p._2)),
      "actions" -> Map()
    )

    PubSubHelper.publish(TOPIC_QUESTION, QuestionMessage(TASK_CREATE, result, id, creator.id, (topicObjects.keySet ++ newTopicIds).toList, score, updated, 0L))

    result
  }

  def getRow(input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.QUESTION)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }
}
