package constants

import utils.Config

object Db {
  lazy val CASSANDRA_KEYSPACE = Config.getString("cassandra.keyspace", "balloon")
  lazy val ES_INDEX = Config.getString("es.index", "balloon")
  lazy val ES_ENABLE = Config.getBoolean("es.enable", true)

  lazy val GRAPH_ENABLE = Config.getBoolean("graph.enable", false)

  lazy val IMAGE_PATH = Config.getString("image.path", "images")
  lazy val IMAGE_START_URL = Config.getString("image.start_url", "images/thumb")
  lazy val THUMB_SIZE = Config.getInt("image.thumb", 100)
  lazy val FULL_WIDTH = Config.getInt("image.full_width", 766)

  lazy val TITAN_CASSANDRA_HOST = Config.getString("graph.titan.cassandra.host", "localhost")
  lazy val TITAN_CASSANDRA_KEYSPACE = Config.getString("graph.titan.cassandra.keyspace", "cct")
  lazy val TITAN_ES_HOST = Config.getString("graph.titan.es.host", "localhost")
  lazy val TITAN_ES_INDEX = Config.getString("graph.titan.es.index", "cct")
}
