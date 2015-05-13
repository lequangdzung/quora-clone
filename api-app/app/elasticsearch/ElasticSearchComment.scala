package elasticsearch

import database.ElasticSearch._
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder

import scala.collection.JavaConversions._

import play.api.libs.json._
import utils.JsonHelper._

object ElasticSearchComment {
  def getComments(query: String = null, start: Int, size: Int, order: String, direction: String) = {
    val total = client.prepareCount(indexName)
      .setTypes(constants.ElasticSearch.INDEX_TYPE_COMMENT)
      .execute()
      .actionGet()
      .getCount


    var searchObject = client.prepareSearch(indexName)
      .setTypes(constants.ElasticSearch.INDEX_TYPE_COMMENT)
      .setFrom(start)
      .setSize(size)
      .addSort(order, SortOrder.ASC)

    if(query != null) searchObject = searchObject.setQuery(QueryBuilders.queryString(query))

    val items = searchObject.execute().actionGet().getHits.getHits.map { response =>
      Json.parse(response.getSourceAsString)
    }

    (total, items)
  }
}
