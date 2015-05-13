package security.oauth2

import scalaoauth2.provider.{AccessToken, AuthInfo, DataHandler}

import models.User
import java.util.{UUID, Date}

class CustomDataHandler extends DataHandler[User] {

//  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean = ???
//
  def findUser(username: String, password: String): Option[User] = {
    val user = User.getUser(username, password)
    user
  }
//
  def createAccessToken(authInfo: AuthInfo[User]): AccessToken = TokenStorage.createAccessToken(authInfo)
//
//  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[AccessToken] = ???
//
//  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): AccessToken = ???
//
//  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] = ???
//
//  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = ???
//
//  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = ???
//
//  def findAccessToken(token: String): Option[AccessToken] = ???
//
//  def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[User]] = ???


  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean  = true

  //def findUser(username: String, password: String): Option[User] = Some(User())

  //def createAccessToken(authInfo: AuthInfo[User]): AccessToken = AccessToken("", Some(""), Some(""), Some(0L), new Date())

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] = Some(AuthInfo(User.getUser("a@luxcer.com", "test").get, "test", None, None))

  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = Some(AuthInfo(User.getUser("a@luxcer.com", "test").get, "test", None, None))

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = User.getUser("a@luxcer.com", "test")

  def findAccessToken(token: String): Option[AccessToken] = TokenStorage.getAccessToken(token)

  def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[User]] = TokenStorage.getAuthByAccessToken(accessToken)

  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[AccessToken] = TokenStorage.getAccessTokenByAuth(authInfo)

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): AccessToken = TokenStorage.refreshAccessToken(refreshToken, authInfo)

}
