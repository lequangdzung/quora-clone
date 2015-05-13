package models

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.querybuilder.QueryBuilder
import scalaoauth2.provider.{AuthInfo, AccessToken}

import database.Cassandra
import constants.Table

object Token extends Follow {
  val ACCESS_TOKEN_TABLE = "access_token"
  val ACCESS_TOKEN_EXPIRE_TABLE = "access_token_expire"
  val AUTH_INFO_TABLE = "auth_info"

  val DEFAULT_EXPIRE = 36000

  def genTokenId = UUIDs.timeBased()

  def storeToken(accessToken: AccessToken, authInfo: AuthInfo[User]) = {
    val authInfoId = UUIDs.timeBased()
    val cqlAuthInfo = QueryBuilder.insertInto(AUTH_INFO_TABLE)
      .value("id", authInfoId)
      .value("user_id", authInfo.user.id)
      .value("client_id", authInfo.clientId)
      .value("scope", authInfo.scope.getOrElse(null))
      .value("redirect_uri", authInfo.redirectUri.getOrElse(null))
      .toString

    Cassandra.session.execute(cqlAuthInfo)

    val cqlAccessToken = QueryBuilder.insertInto(ACCESS_TOKEN_TABLE)
      .value("id", accessToken.token)
      .value("refresh_token", UUID.fromString(accessToken.refreshToken.getOrElse(null)))
      .value("auth_info_id", authInfoId)
      .value("scope", accessToken.scope.getOrElse(null))
      .value("expires_in", accessToken.expiresIn.getOrElse(null))
      .value("created_at", accessToken.createdAt)
      .using(QueryBuilder.timestamp(DEFAULT_EXPIRE))

    Cassandra.session.execute(cqlAccessToken)

    // update expire in column family
    val cqlAccessTokenExpire = QueryBuilder.insertInto(ACCESS_TOKEN_EXPIRE_TABLE)
      .value("key", "all")
      .value("expire", new Date(accessToken.createdAt.getTime + accessToken.expiresIn.getOrElse(DEFAULT_EXPIRE).asInstanceOf[Long] * 1000))
      .value("token_id", accessToken.token)
      .toString

    Cassandra.session.execute(cqlAccessTokenExpire)
  }

  def delToken(id: String) = {
    Cassandra.session.execute(
      QueryBuilder.delete().all()
        .from(ACCESS_TOKEN_TABLE)
        .where(QueryBuilder.eq("id", id))
    )
  }

  def getActiveTokens = {
    var cql = QueryBuilder.select("token_id")
      .from(ACCESS_TOKEN_EXPIRE_TABLE)
      .toString

    val idsRows = Cassandra.session.execute(cql).all()

    val ids = idsRows.map(_.getString("token_id"))

    cql = QueryBuilder.select()
      .all()
      .from(ACCESS_TOKEN_TABLE)
      .where(QueryBuilder.in("id", ids.toSeq:_*))
      .toString

    val tokenRows = Cassandra.session.execute(cql).all()

    val authIds = tokenRows.map(_.getUUID("auth_info_id"))

    cql = QueryBuilder.select()
      .all()
      .from(AUTH_INFO_TABLE)
      .where(QueryBuilder.in("id", authIds.toSeq:_*))
      .toString

    val authRows = Cassandra.session.execute(cql).all()

    val userIds = authRows.map(_.getUUID("user_id"))

    cql = QueryBuilder.select()
      .all()
      .from(Table.USER)
      .where(QueryBuilder.in("id", userIds.toSeq:_*))
      .toString

    val userRows = Cassandra.session.execute(cql).all()

    val follows = getFollowTarget(userIds.toSet)

    val users = userRows.map { row =>
      val id = row.getUUID("id")
      val obj = User(
        row.getUUID("id"),
        row.getString("email"),
        row.getString("password"),
        row.getString("display"),
        row.getString("avatar"),
        row.getBool("status"),
        asScalaSet(row.getSet("permissions", classOf[java.lang.String])).toSet,
        row.getString("fbid"),
        row.getDate("created"),
        row.getLong("follower_count"),
        row.getLong("unread")
      )

      (id, obj)
    }.toMap



    val auths = authRows.map { row =>
      val id = row.getUUID("id")
      val obj = AuthInfo(
        users.get(row.getUUID("user_id")).get,
        row.getString("client_id"),
        Some(row.getString("scope")),
        Some(row.getString("redirect_uri"))
      )
      (id, obj)
    }.toMap

    val tokens = tokenRows.map { row =>
      val id = row.getString("id")
      val obj = AccessToken(
        id.toString,
        Some(row.getUUID("refresh_token").toString),
        Some(row.getString("scope")),
        Some(row.getLong("expires_in")),
        row.getDate("created_at")
      )

      (id.toString, (obj.refreshToken.get, obj, auths.get(row.getUUID("auth_info_id")).get))
    }.toMap

    tokens
  }
}