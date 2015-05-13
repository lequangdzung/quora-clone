package security.oauth2


import java.util.{UUID, Date}

import scalaoauth2.provider._

import constants.PubSub._
import constants.Task._
import models.{Token, User}
import pubsub.{TokenData, PubSubHelper}

object TokenStorage {
  // accessToken, (refreshToken, ...)
  var tokens: Map[String, (String, AccessToken, AuthInfo[User])] = Map()

  def init = {
    tokens = Token.getActiveTokens
  }

  def del(id: String) = tokens -= id

  def getTokens = tokens

  def createAccessToken(authInfo: AuthInfo[User]): AccessToken = {
//    println("create access token by auth: " + authInfo)
    val tokenId = Token.genTokenId.toString
    val refreshToken = Token.genTokenId.toString

    val accessToken = AccessToken(tokenId, Some(refreshToken), authInfo.scope, Some(36000), new Date())
    tokens = tokens + (tokenId -> (refreshToken, accessToken, authInfo))

    PubSubHelper.publish(TOPIC_ACCESS_TOKEN, TokenData(CREATE, accessToken, authInfo))

    tokens(tokenId)._2
  }

  def createAccessTokenFromUser(user: User) = {
    val authInfo = AuthInfo(user, "temp", Some(""), Some(""))

    val tokenId = Token.genTokenId.toString
    val refreshToken = Token.genTokenId.toString
    val accessToken = AccessToken(tokenId, Some(refreshToken), authInfo.scope, Some(36000), new Date())
    tokens = tokens + (tokenId -> (refreshToken, accessToken, authInfo))

    PubSubHelper.publish(TOPIC_ACCESS_TOKEN, TokenData(CREATE, accessToken, authInfo))

    tokens(tokenId)._2
  }

  def createFbAccessToken(user: User, tokenId: String) = {
    // trying to create authInfo
    val authInfo = AuthInfo(user, "temp", Some(""), Some(""))

    val refreshToken = Token.genTokenId.toString

    val accessToken = AccessToken(tokenId, Some(refreshToken), authInfo.scope, Some(36000), new Date())
    tokens = tokens + (tokenId -> (refreshToken, accessToken, authInfo))

    PubSubHelper.publish(TOPIC_ACCESS_TOKEN, TokenData(CREATE, accessToken, authInfo))

    tokens(tokenId)._2
  }

  def getAccessToken(token: String) = {
//    println("get access token by token:" + token)
    //println("tokenId is :" + token)
    //println("token found is:" + tokens)
    //println("token found is:" + tokens.get(token).map(_._2))
    tokens.get(token).map(_._2)
  }

  def getAuthByAccessToken(accessToken: AccessToken) = {
    //println("getAuthByAccessToken")
//    println("get auth by access token: " + accessToken)
    tokens.get(accessToken.token).map { p =>
      val accessToken = p._2
      var newAccessToken = AccessToken(accessToken.token, accessToken.refreshToken, accessToken.scope, accessToken.expiresIn, new Date())
      tokens = tokens + (accessToken.token -> (accessToken.refreshToken.get, newAccessToken, p._3))
      p._3
    }
  }

  def getAccessTokenByAuth(authInfo: AuthInfo[User]) = {
//    println("get access token by auth: " + authInfo)
    tokens.find(p => p._2._3 == authInfo).map(_._2._2)
  }

  def refreshAccessToken(refreshToken: String, authInfo: AuthInfo[User]) = {
    //println("refresh access token refreshtoken: " + refreshToken)
//    tokens.find { p =>
//      (p._2._1 == refreshToken) && (p._2._3 == authInfo)
//    }.get._2._2

    tokens.find { p =>
      (p._2._1 == refreshToken) && (p._2._3 == authInfo)
    }.map { item =>
      // update access token create time here
      val accessToken = item._2._2
      val newAccessToken = AccessToken(accessToken.token, accessToken.refreshToken, accessToken.scope, accessToken.expiresIn, new Date())
      tokens = tokens + (accessToken.token -> (accessToken.refreshToken.get, newAccessToken, item._2._3))
      newAccessToken
    }.get
  }

  def resetNotify(userId: UUID) = {
    tokens.filter(p => p._2._3.user.id == userId).map { token =>
      val user = token._2._3.user

      val newUser = User(user.id, user.email, user.password, user.display, user.avatar, user.status, user.permissions, user.fbid, user.created, user.followerCount, 0L)
      val authInfo = token._2._3
      tokens = tokens + (token._1 -> (token._2._1, token._2._2, AuthInfo(newUser, authInfo.clientId, authInfo.scope, authInfo.redirectUri)))
    }
  }

  def addNotify(userId: UUID) = {
    tokens.filter(p => p._2._3.user.id == userId).map { token =>
      val user = token._2._3.user

      val newUser = User(user.id, user.email, user.password, user.display, user.avatar, user.status, user.permissions, user.fbid, user.created, user.followerCount, user.unread + 1)
      val authInfo = token._2._3
      tokens = tokens + (token._1 -> (token._2._1, token._2._2, AuthInfo(newUser, authInfo.clientId, authInfo.scope, authInfo.redirectUri)))
    }
  }
}

object TokenHelper {

}
