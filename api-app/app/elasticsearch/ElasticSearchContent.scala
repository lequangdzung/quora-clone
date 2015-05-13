package elasticsearch

import database.ElasticSearch._
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{FilterBuilder, QueryBuilders}
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.search.sort.SortOrder

import scala.collection.JavaConversions._

import play.api.libs.json._
import utils.JsonHelper._
import java.util.UUID
import play.api.mvc.RequestHeader

object ElasticSearchContent {
  def getContents(query: String = null, field: String, fieldValue: String, start: Int, size: Int, order: String, direction: String)(implicit request: RequestHeader) = {
    val total = client.prepareCount(getIndexName(request))
      .setTypes(constants.ElasticSearch.INDEX_TYPE_CONTENT)
      .execute()
      .actionGet()
      .getCount


    var searchObject = client.prepareSearch(getIndexName(request))
      .setTypes(constants.ElasticSearch.INDEX_TYPE_CONTENT)
      .setFrom(start)
      .setSize(size)

    if(order != null) {
      if(direction == "ASC") searchObject = searchObject.addSort(order, SortOrder.ASC)
      else searchObject = searchObject.addSort(order, SortOrder.DESC)
    }

    if(query != null) {
      searchObject = searchObject.setQuery(QueryBuilders.queryString(query))
    }

    val items = searchObject.execute().actionGet().getHits.getHits.map { response =>
      Json.parse(response.getSourceAsString)
    }

    (total, items)
  }
}
