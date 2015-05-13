package database

import scala.collection.JavaConversions._

import org.elasticsearch.node.NodeBuilder._
import org.elasticsearch.client.Client
import org.elasticsearch.node.Node
import org.elasticsearch.common.xcontent.XContentFactory._

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.search.sort.SortOrder

import constants.Db._
import play.api.libs.json._
import utils.JsonHelper._
import play.api.mvc.RequestHeader
import utils.KeyHelper
import java.util.UUID
import org.apache.lucene.queryparser.xml.FilterBuilder
import org.elasticsearch.common.settings.ImmutableSettings
import scala.collection.mutable
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest

object ElasticSearch {
  val indexName = ES_INDEX

  var client: Client = _
  var node: Node = _



  def getIndexName(request: RequestHeader) = {
    ES_INDEX
  }

  def start = {
    client = new TransportClient()
      .addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

    val exist = client.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists
    if(!exist) {
      val sources =jsonBuilder()
        .startObject()
          .startObject("settings")
            .startObject("analysis")
              .startObject("analyzer")
                .startObject("folding")
                  .field("filter", bufferAsJavaList(mutable.Buffer[String]("lowercase", "asciifolding")))
                  .field("tokenizer","standard")
                .endObject()
              .endObject()
            .endObject()
          .endObject()
          .startObject("mappings")
            .startObject("question")
              .startObject("properties")
                .startObject("title")
                  .field("type", "string")
                  .field("analyzer", "folding")
                .endObject()
              .endObject()
            .endObject()
            .startObject("topic")
              .startObject("properties")
                .startObject("name")
                  .field("type", "string")
                  .field("analyzer", "folding")
                .endObject()
              .endObject()
            .endObject()
          .endObject()
        .endObject()
        .string()

      val result = client.admin().indices().prepareCreate(indexName).setSource(sources).execute().actionGet()
    }
    //node = nodeBuilder().node()
    //client = node.client()
  }

  def index(indexType: String, id: String, data: Map[String, AnyRef])(implicit request: RequestHeader) = {
    var builder = jsonBuilder.startObject()
    builder = data.foldLeft(builder)((b, p) => {
      b.field(p._1, p._2)
    })

    client.prepareIndex(getIndexName(request), indexType, id).setSource(builder).execute()
  }

  def jsonIndex(indexType: String, id: String, data: Map[String, Any]) = {
    client.prepareIndex(ES_INDEX, indexType, id).setSource(Json.toJson(data).toString()).execute()
  }

  def updateIndex(indexType: String, id: String, data: Map[String, Any])(implicit request: RequestHeader) = {
    client.prepareUpdate(getIndexName(request), indexType, id).setDoc(Json.toJson(data).toString()).execute()
  }

  def search(indexType: String, fields: Seq[String], query: String, highlight_field: String, size: Int, start: Int, excludeIds: Seq[String] = Seq()) = {
    var searchObject = client.prepareSearch(ES_INDEX)
      .setTypes(indexType)
      .setFrom(start)
      .setSize(size)
      .setQuery(QueryBuilders.multiMatchQuery(query, fields:_*).analyzer("folding"))

    if(excludeIds.size > 0) {
      val filters = FilterBuilders.boolFilter().mustNot(
        FilterBuilders.idsFilter().addIds(excludeIds:_*)
      )
      searchObject = searchObject.setPostFilter(filters)
    }

    if(highlight_field != null) {
      searchObject =
        searchObject.addHighlightedField(highlight_field)
          .setHighlighterPreTags("<b>")
          .setHighlighterPostTags("</b>")
    }

    searchObject.execute().actionGet().getHits.getHits
  }

  def stop = {
    node.close()
  }
}
