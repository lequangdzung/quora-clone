package security.oauth2

import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.Future
import models.User

import java.net.{URI, URL, URLEncoder, URLDecoder}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.RequestHeader

object Facebook {
  def validateToken(token: String) = {

    val url = new URL(s"https://graph.facebook.com/debug_token?input_token=$token&access_token=385886264899275" + URLEncoder.encode("|", "UTF-8") + "87df6f39840ecfddb9bf91965f5b40d1")

    val holder: WSRequestHolder = WS.url(url.toString)

    holder.get().map { response =>
      println("json when validateToken: " + response.json)
      response.json
    }
  }

  def getUser(token: String)(implicit request: RequestHeader) = {
    val encodeToken = URLEncoder.encode(token)
    val url = s"https://graph.facebook.com/me?access_token=$encodeToken"

    val holder: WSRequestHolder = WS.url(url)

    holder.get().map { response =>
      println("da vao getUser of FB: " + response.json)
      val json = response.json
      val fbid = (json \ "id").as[String]
      val email = (json \ "email").as[String]
      val display = (json \ "name").as[String]
      //val alias = (json \ "username").as[String]

      // try to get user by email
      User._findUser(email).map { userId =>
        println("userId is: " + userId)
        User._getUser(userId).get
      }.getOrElse {
        // try to register user here
        User.addUser(email, "", display, fbid)
      }
    }
  }
}
