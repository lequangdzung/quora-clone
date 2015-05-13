package elasticsearch

import database.ElasticSearch._
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import org.elasticsearch.search.sort.SortOrder

import scala.collection.JavaConversions._

import play.api.libs.json._
import utils.JsonHelper._
import java.util.UUID
import play.api.mvc.RequestHeader

object ElasticSearchTag {
  def getTags(query: String = null, start: Int, size: Int, order: String, direction: String, excludeIds: List[UUID] = List())(implicit request: RequestHeader) = {
    val total = client.prepareCount(getIndexName(request))
      .setTypes(constants.ElasticSearch.INDEX_TYPE_TAG)
      .execute()
      .actionGet()
      .getCount


    var searchObject = client.prepareSearch(getIndexName(request))
      .setTypes(constants.ElasticSearch.INDEX_TYPE_TAG)
      .setFrom(start)
      .setSize(size)
      .addSort(order, SortOrder.ASC)

    if(query != null) searchObject = searchObject.setQuery(QueryBuilders.queryString(query))

    if(!excludeIds.isEmpty) {
      val filterBuilder = FilterBuilders.notFilter(
        FilterBuilders.idsFilter(excludeIds.map(_.toString).toSeq:_*)
      )
      searchObject.setPostFilter(filterBuilder)
    }

    val items = searchObject.execute().actionGet().getHits.getHits.map { response =>
      Json.parse(response.getSourceAsString)
    }

    (total, items)
  }

  def searchTags(query: String = null, start: Int, size: Int, excludeIds: List[UUID] = List())(implicit request: RequestHeader) = {

    var searchObject = client.prepareSearch(getIndexName(request))
      .setTypes(constants.ElasticSearch.INDEX_TYPE_TAG)
      .setFrom(start)
      .setSize(size)
      //.addSort(order, SortOrder.ASC)
      //.addSort()

    if(query != null) searchObject = searchObject.setQuery(
//      QueryBuilders.queryString("*" + query + "*")
//      QueryBuilders.fuzzyLikeThisQuery("name", "info")     // Fields
//        .likeText(query)                 // Text
//        .maxQueryTerms(2)
      //QueryBuilders.termQuery("name", query + "*")
      QueryBuilders.fuzzyLikeThisQuery("name", "info").likeText(query)
    )

    if(!excludeIds.isEmpty) {
      val filterBuilder = FilterBuilders.notFilter(
        FilterBuilders.idsFilter(excludeIds.map(_.toString).toSeq:_*)
      )
      searchObject.setPostFilter(filterBuilder)
    }

    val items = searchObject.execute().actionGet().getHits.getHits.map { response =>

      val itemData = response.getSource.toMap + ("id" -> response.getId)
//      println("item data is: " + itemData)
//      println("source is: " + response.getSourceAsString)
//      Json.parse(response.getSourceAsString)
      itemData
    }

    println("items is: " + items)

    items
  }
}
