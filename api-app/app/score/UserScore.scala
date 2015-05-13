package score

import java.util.UUID
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder
import database.Cassandra
import constants.Table
import constants.Score
import java.util

trait UserScore {
  def postUp(contentId: UUID, userId: UUID)
  def postDown(contentId: UUID, userId: UUID)
  def commentUp(commentId: UUID, userId: UUID)
  def commentDown(commentId: UUID, userId: UUID)
}

object UserScore extends UserScore{
  def postUp(contentId: UUID, userId: UUID) = {
    // get content first
    Cassandra.session.execute(
      QueryBuilder.select("creator", "tags")
        .from(Table.CONTENT)
        .where(QueryBuilder.eq("id", contentId))
    ).all().collectFirst{ case r => r}.map { row =>
      // userId first
      val creator = row.getUUID("creator")

      if(userId != creator) {
        val tags = row.getSet("tags", classOf[UUID]).toIterable.toSeq

        // get tags name from ids
        val exitTags = Cassandra.session.execute(
          QueryBuilder.select().all()
            .from(Table.TAG)
            .where(QueryBuilder.in("id", tags:_*))
        ).all().map { tagRow =>
          Cassandra.session.execute(
            QueryBuilder.update(Table.SCORE_COUNTER)
              .`with`(QueryBuilder.incr("score"))
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.eq("tag", tagRow.getString("name")))
          )

          tagRow.getString("name")
        }.toSeq ++ Seq(Score.USER_SCORE)

        Cassandra.session.execute(
          QueryBuilder.update(Table.SCORE_COUNTER)
            .`with`(QueryBuilder.incr("score"))
            .where(QueryBuilder.eq("key", creator.toString))
            .and(QueryBuilder.eq("tag", Score.USER_SCORE))
        )

        val scoreUpdate = new util.TreeMap[String, java.lang.Long]()
        // after increase, get and update to user table
        Cassandra.session.execute(
          QueryBuilder.select("score", "tag")
            .from(Table.SCORE_COUNTER)
            .where(QueryBuilder.eq("key", creator.toString))
            .and(QueryBuilder.in("tag", exitTags:_*))
        ).all().map { r =>
          scoreUpdate.put(r.getString("tag"), r.getLong("score"))
        }

        println("Cql is: " + QueryBuilder.update(Table.USER)
          .`with`(QueryBuilder.putAll("score", scoreUpdate))
          .where(QueryBuilder.eq("id", creator.toString)).toString)
        Cassandra.session.execute(
          QueryBuilder.update(Table.USER)
            .`with`(QueryBuilder.putAll("score", scoreUpdate))
            .where(QueryBuilder.eq("id", creator))
        )
      }
    }
  }

  def postDown(contentId: UUID, userId: UUID) = {
    // get content first
    Cassandra.session.execute(
      QueryBuilder.select("creator", "tags")
        .from(Table.CONTENT)
        .where(QueryBuilder.eq("id", contentId))
    ).all().collectFirst{ case r => r}.map { row =>
    // userId first
      val creator = row.getUUID("creator")

      if(userId != creator) {
        val tags = row.getSet("tags", classOf[UUID]).toIterable.toSeq

        // get tags name from ids
        val exitTags = Cassandra.session.execute(
          QueryBuilder.select().all()
            .from(Table.TAG)
            .where(QueryBuilder.in("id", tags:_*))
        ).all().map { tagRow =>
          Cassandra.session.execute(
            QueryBuilder.update(Table.SCORE_COUNTER)
              .`with`(QueryBuilder.incr("score"))
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.eq("tag", tagRow.getString("name")))
          )

          tagRow.getString("name")
        }.toSeq ++ Seq(Score.USER_SCORE)

        Cassandra.session.execute(
          QueryBuilder.update(Table.SCORE_COUNTER)
            .`with`(QueryBuilder.decr("score"))
            .where(QueryBuilder.eq("key", creator.toString))
            .and(QueryBuilder.eq("tag", Score.USER_SCORE))
        )

        val scoreUpdate = new util.TreeMap[String, java.lang.Long]()
        // after increase, get and update to user table
        Cassandra.session.execute(
          QueryBuilder.select("score", "tag")
            .from(Table.SCORE_COUNTER)
            .where(QueryBuilder.eq("key", creator.toString))
            .and(QueryBuilder.in("tag", exitTags:_*))
        ).all().map { r =>
          scoreUpdate.put(r.getString("tag"), r.getLong("score"))
        }

        Cassandra.session.execute(
          QueryBuilder.update(Table.USER)
            .`with`(QueryBuilder.putAll("score", scoreUpdate))
            .where(QueryBuilder.eq("id", creator))
        )
      }
    }
  }

  def commentUp(commentId: UUID, userId: UUID) = {
    Cassandra.session.execute(
      QueryBuilder.select("creator", "content")
        .from(Table.COMMENT)
        .where(QueryBuilder.eq("id", commentId))
    ).all().collectFirst{ case r => r}.map { commentRow =>
      val creator = commentRow.getUUID("creator")
      val contentId = commentRow.getUUID("content")

      // get content first
      Cassandra.session.execute(
        QueryBuilder.select("creator", "tags")
          .from(Table.CONTENT)
          .where(QueryBuilder.eq("id", contentId))
      ).all().collectFirst{ case r => r}.map { row =>
      // userId first

        if(userId != creator) {
          val tags = row.getSet("tags", classOf[UUID]).toIterable.toSeq

          // get tags name from ids
          val exitTags = Cassandra.session.execute(
            QueryBuilder.select().all()
              .from(Table.TAG)
              .where(QueryBuilder.in("id", tags:_*))
          ).all().map { tagRow =>
            Cassandra.session.execute(
              QueryBuilder.update(Table.SCORE_COUNTER)
                .`with`(QueryBuilder.incr("score"))
                .where(QueryBuilder.eq("key", creator.toString))
                .and(QueryBuilder.eq("tag", tagRow.getString("name")))
            )

            tagRow.getString("name")
          }.toSeq ++ Seq(Score.USER_SCORE)

          Cassandra.session.execute(
            QueryBuilder.update(Table.SCORE_COUNTER)
              .`with`(QueryBuilder.incr("score"))
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.eq("tag", Score.USER_SCORE))
          )

          val scoreUpdate = new util.TreeMap[String, java.lang.Long]()
          // after increase, get and update to user table
          Cassandra.session.execute(
            QueryBuilder.select("score", "tag")
              .from(Table.SCORE_COUNTER)
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.in("tag", exitTags:_*))
          ).all().map { r =>
            scoreUpdate.put(r.getString("tag"), r.getLong("score"))
          }

          Cassandra.session.execute(
            QueryBuilder.update(Table.USER)
              .`with`(QueryBuilder.putAll("score", scoreUpdate))
              .where(QueryBuilder.eq("id", creator))
          )
        }
      }
    }
  }

  def commentDown(commentId: UUID, userId: UUID) = {
    Cassandra.session.execute(
      QueryBuilder.select("creator", "content")
        .from(Table.COMMENT)
        .where(QueryBuilder.eq("id", commentId))
    ).all().collectFirst{ case r => r}.map { commentRow =>
      val creator = commentRow.getUUID("creator")
      val contentId = commentRow.getUUID("content")

      // get content first
      Cassandra.session.execute(
        QueryBuilder.select("creator", "tags")
          .from(Table.CONTENT)
          .where(QueryBuilder.eq("id", contentId))
      ).all().collectFirst{ case r => r}.map { row =>
      // userId first

        if(userId != creator) {
          val tags = row.getSet("tags", classOf[UUID]).toIterable.toSeq

          // get tags name from ids
          val exitTags = Cassandra.session.execute(
            QueryBuilder.select().all()
              .from(Table.TAG)
              .where(QueryBuilder.in("id", tags:_*))
          ).all().map { tagRow =>
            Cassandra.session.execute(
              QueryBuilder.update(Table.SCORE_COUNTER)
                .`with`(QueryBuilder.incr("score"))
                .where(QueryBuilder.eq("key", creator.toString))
                .and(QueryBuilder.eq("tag", tagRow.getString("name")))
            )

            tagRow.getString("name")
          }.toSeq ++ Seq(Score.USER_SCORE)

          Cassandra.session.execute(
            QueryBuilder.update(Table.SCORE_COUNTER)
              .`with`(QueryBuilder.decr("score"))
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.eq("tag", Score.USER_SCORE))
          )

          val scoreUpdate = new util.TreeMap[String, java.lang.Long]()
          // after increase, get and update to user table
          Cassandra.session.execute(
            QueryBuilder.select("score", "tag")
              .from(Table.SCORE_COUNTER)
              .where(QueryBuilder.eq("key", creator.toString))
              .and(QueryBuilder.in("tag", exitTags:_*))
          ).all().map { r =>
            scoreUpdate.put(r.getString("tag"), r.getLong("score"))
          }

          Cassandra.session.execute(
            QueryBuilder.update(Table.USER)
              .`with`(QueryBuilder.putAll("score", scoreUpdate))
              .where(QueryBuilder.eq("id", creator))
          )
        }
      }
    }
  }
}