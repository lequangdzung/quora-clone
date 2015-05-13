package models

import java.util.UUID
import database.Cassandra
import com.datastax.driver.core.querybuilder.QueryBuilder
import constants.Table

object Target {
  def getRow(target: String, input: String) = {
    try {
      val id = UUID.fromString(input)

      val row = Cassandra.session.execute(
        QueryBuilder.select().all().from(target)
          .where(QueryBuilder.eq("id", id))
      ).one()

      if(row != null) Some(row) else None
    }
    catch {
      case e: Exception => None
    }
  }
}
