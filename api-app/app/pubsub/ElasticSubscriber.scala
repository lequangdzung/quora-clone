package pubsub

import constants.PubSub._
import java.util.UUID
import database.ElasticSearch
import play.api.mvc.RequestHeader

case class ElasticIndex(indexType: String, id: UUID, data: Map[String, Any], request: RequestHeader)
case class ElasticStringIndex(indexType: String, id: String, data: Map[String, Any], request: RequestHeader)
case class ElasticUpdate(indexType: String, id: UUID, data: Map[String, Any], request: RequestHeader)

class ElasticSubscriber extends Subscriber(TOPIC_ELATICSEAERCH){
  def process(data: Any) = data match {
    case msg: ElasticIndex => ElasticSearch.jsonIndex(msg.indexType, msg.id.toString, msg.data)
    case msg: ElasticStringIndex => ElasticSearch.jsonIndex(msg.indexType, msg.id, msg.data)
    case msg: ElasticUpdate =>
      println("ro rang co vay dao: " + msg)
      ElasticSearch.updateIndex(msg.indexType, msg.id.toString, msg.data)(msg.request)
  }
}