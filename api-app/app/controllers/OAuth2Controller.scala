package controllers

import play.api._
import play.api.mvc._
import scalaoauth2.provider._

import security.oauth2._
import models.{User, Token}
import java.util.UUID
import play.api.libs.json.{JsNull, Json}

import utils.JsonHelper._
import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object OAuth2Controller extends Controller with MyOAuth2Provider {
  def signIn = Action { implicit request =>
     issueAccessToken(new CustomDataHandler())
  }

  def signInFb = Action.async(parse.json) { implicit request =>

    (request.body \ "token").asOpt[String].map { token =>
      // need to validate this token
      Facebook.validateToken(token).map { response =>
        val isValid = (response \ "data" \ "is_valid").as[Boolean]
        if(isValid) {
          val result = Facebook.getUser(token).map { user =>
            // try to login here
            val accessToken = TokenStorage.createFbAccessToken(user, token)
            Json.obj(
              "access_token" -> accessToken.token,
              "refresh_token" -> accessToken.refreshToken.get,
              "required_pass" -> (user.password == "" || user.password == null),
              "user" -> Json.obj(
                "display" -> user.display,
                "avatar" -> user.avatar,
                "id" -> user.id,
                "email" -> user.email,
                "followerCount" -> user.followerCount,
                "unread" -> user.unread,
                "required_pass" -> (user.password == "" || user.password == null)
              )
            )
          }
          val ret = Await.result(result, 5 seconds)
          Ok(ret)
        } else {
          Unauthorized
        }
      }
    }.getOrElse(Future(BadRequest))
  }

  def signOut = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      TokenStorage.getTokens.filter(p => p._2._3.user.id == authInfo.user.id).map { token =>
        Token.delToken(token._1)
        TokenStorage.del(token._1)
      }
      NoContent
    }
  }
}
