package utils
import scala.collection.JavaConversions._

import play.api.libs.json._

import com.datastax.driver.core.{Session, Cluster, querybuilder}
import com.datastax.driver.core.querybuilder._
import com.datastax.driver.core.utils._

import database.Cassandra
import constants.Table
import utils.JsonHelper._

object Alias {
  val USER_KEY = "USER"
  val TOPIC_KEY = "TOPIC"
  val CONTENT_KEY = "CONTENT"
  val QUESTION_KEY = "QUESTION"

  def convert(in: String, key: String, separator: String = "-") = {
    // trying to get exits alias first
    var alias = to(in, separator)
    var i = 1
    while (_isExits(alias, key)) {
      alias = to(in + " " + i, separator)
      i += 1
    }

    alias
  }

  def _isExits(alias: String, key: String) = {
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.ALIAS)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("alias", alias))
    ).iterator().hasNext
  }

  def save(key: String, alias: String, data: Map[String, Any]) = {
    Cassandra.session.execute(
      QueryBuilder.update(Table.ALIAS)
        .`with`(QueryBuilder.set("data", Json.toJson(data).toString()))
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("alias", alias))
    )
  }

  def get(key: String, alias: String) = {
    Cassandra.session.execute(
      QueryBuilder.select().all()
        .from(Table.ALIAS)
        .where(QueryBuilder.eq("key", key))
        .and(QueryBuilder.eq("alias", alias))
    ).all().collectFirst {case v => v}.map { alias =>
      val data = Json.parse(alias.getString("data"))
      data.asInstanceOf[JsObject].value.map(p=> (p._1, p._2.asInstanceOf[JsString].value)).toMap
    }
  }

  def to(in: String, separator: String = "-") = {
    val charReg = """[\"\'`\.,:;\{\}\[\]\+=\*\^&%\$#\!\~\(\)\?<>/\\|]"""
    var ret = in.replaceAll(charReg, "")

    val wordReg = """\s((?i)a|an|as|at|before|but|by|for|from|is|in|into|like|of|on|onto|per|since|than|this|that|to|up|via|with)\s"""
    ret = ret.replaceAll(wordReg, " ")
    ret = ret.replaceAll(wordReg, " ")

    val dashReg = """[_\s]"""
    ret = ret.replaceAll(dashReg, separator)

    ret
  }
}
