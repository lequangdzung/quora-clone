package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder
import constants.Table
import database.Cassandra

object UserAction {
  val actions = Seq("vote", "comment", "bookmark", "follow", "answer", "comment", "promote")
  def get(userId: UUID, target: String, target_Ids: Set[UUID]) = {
    Cassandra.session.execute(
      QueryBuilder.select((actions ++ Seq("target_id")):_*)
        .from(Table.USER_ACTION)
        .where(QueryBuilder.eq("user_id", userId))
        .and(QueryBuilder.eq("target", target))
        .and(QueryBuilder.in("target_id", target_Ids.toSeq:_*))
    ).all().map { p =>
      val target_id = p.getUUID("target_id")
      val data: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map()
      actions.foreach { action =>
        if(p.getString(action) != null) data(action) = p.getString(action)
      }
      (target_id, data.toMap)
    }.toMap
  }
  
  def add(userId: UUID, target: String, target_id: UUID, name: String, value: String) = {
    Cassandra.session.execute(
      QueryBuilder.insertInto(Table.USER_ACTION)
        .value("user_id", userId)
        .value("target", target)
        .value("target_id", target_id)
        .value(name, value)
    )
  }

  def delete(userId: UUID, target: String, target_id: UUID, column: String) = {
    Cassandra.session.execute(
      QueryBuilder.delete(column).from(Table.USER_ACTION)
        .where(QueryBuilder.eq("user_id", userId))
        .and(QueryBuilder.eq("target", target))
        .and(QueryBuilder.eq("target_id", target_id))
    )
  }
}
