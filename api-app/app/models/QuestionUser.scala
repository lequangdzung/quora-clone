package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder
import constants.Table
import constants.PubSub._
import database.Cassandra
import pubsub.{QuestionDownUserMessage, QuestionUpUserMessage, PubSubHelper}
import utils.{PrefixHelper, Score}

object QuestionUser extends Counter {
  def add(questionId: UUID, userId: UUID, action: String, updated: Date, isCreated: Boolean = false) = {
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.QUESTION_USER)
        .where(QueryBuilder.eq("question_id", questionId))
        .and(QueryBuilder.eq("user_id", userId))
    ).one()

    if((row == null || row.getLong("time") == 0) && !isCreated) {
      val value = incr(Table.QUESTION, "user_count", questionId)

      val QuestionRow = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.QUESTION)
          .where(QueryBuilder.eq("id", questionId))
      ).one()

      val newScore = Score.hot(value, QuestionRow.getDate("updated"))
      val step = newScore - QuestionRow.getLong("score")
      incr(Table.QUESTION, "score", questionId, step)
      PubSubHelper.publish(TOPIC_QUESTION, QuestionUpUserMessage(questionId, userId, updated, QuestionRow))
    }

    Cassandra.session.execute(
      QueryBuilder.update(Table.QUESTION_USER)
        .`with`(QueryBuilder.incr("time"))
        .where(QueryBuilder.eq("question_id", questionId))
        .and(QueryBuilder.eq("user_id", userId))
    )
  }

  def delete(questionId: UUID, userId: UUID, action: String, updated: Date) = {
    val row = Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.QUESTION_USER)
        .where(QueryBuilder.eq("question_id", questionId))
        .and(QueryBuilder.eq("user_id", userId))
    ).one()

    if(row != null && row.getLong("time") == 1) {
      val value = decr(Table.QUESTION, "user_count", questionId)

      val QuestionRow = Cassandra.session.execute(
        QueryBuilder.select().all().from(Table.QUESTION)
          .where(QueryBuilder.eq("id", questionId))
      ).one()

      val newScore = Score.hot(value, QuestionRow.getDate("updated"))
      val step = newScore - QuestionRow.getLong("score")
      incr(Table.QUESTION, "score", questionId, step)
      PubSubHelper.publish(TOPIC_QUESTION, QuestionDownUserMessage(questionId, userId, updated, QuestionRow))
    }

    Cassandra.session.execute(
      QueryBuilder.update(Table.QUESTION_USER)
        .`with`(QueryBuilder.decr("time"))
        .where(QueryBuilder.eq("question_id", questionId))
        .and(QueryBuilder.eq("user_id", userId))
    )
  }
}
