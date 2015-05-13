package database

import com.datastax.driver.core.{Session, Cluster, querybuilder}
import com.datastax.driver.core.querybuilder._
import com.datastax.driver.core.utils._

import scala.collection.JavaConversions._
import java.util
import constants.Db._
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping

object Cassandra {
  var cluster : Cluster = _

  var session: Session = _

  var manager: MappingManager = _

  def connect(node: String) = {
    cluster = Cluster.builder()
      .addContactPoint(node).build()
    session = cluster.connect(CASSANDRA_KEYSPACE)

    manager = new mapping.MappingManager(session)
  }

  def close = {
    cluster.close()
  }
}