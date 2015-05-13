package pubsub

import java.util.{Date, UUID}

import constants.PubSub._
import constants.Table
import database.Cassandra
import com.datastax.driver.core.querybuilder.QueryBuilder
import models.UserAction

case class FollowMessage(userId: UUID, target: String, target_id: UUID, updated: Date, about: String = "follow")
case class UnFollowMessage(userId: UUID, target: String, target_id: UUID)

class FollowSubscriber extends Subscriber(TOPIC_FOLLOW){
  def process(data: Any) = data match {
    case msg: FollowMessage => FollowProcessor.followTarget(msg)
    case msg: UnFollowMessage => FollowProcessor.unFollowTarget(msg)
  }
}

object FollowProcessor {
  def followTarget(msg: FollowMessage) = {
    Cassandra.session.execute(
      QueryBuilder.update(Table.USER_FOLLOW)
        .`with`(QueryBuilder.set("updated", msg.updated))
        .and(QueryBuilder.set("about", msg.about))
        .where(QueryBuilder.eq("user_id", msg.userId))
        .and(QueryBuilder.eq("target", msg.target))
        .and(QueryBuilder.eq("target_id", msg.target_id))
    )

    UserAction.add(msg.userId, msg.target, msg.target_id, "follow", "Y")
  }

  def unFollowTarget(msg: UnFollowMessage) = {
    Cassandra.session.execute(
      QueryBuilder.delete().from(Table.USER_FOLLOW)
        .where(QueryBuilder.eq("user_id", msg.userId))
        .and(QueryBuilder.eq("target", msg.target))
        .and(QueryBuilder.eq("target_id", msg.target_id))
    )

    UserAction.delete(msg.userId, msg.target, msg.target_id, "follow")
  }
}