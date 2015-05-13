package restapis

import java.util.UUID

import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Reads._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

import scalaoauth2.provider._
import security.oauth2.CustomDataHandler

import constants.Content._
import models._
import utils.JsonHelper._
import validation.Helper._
import utils.{KeyHelper, Alias}

object CommentRest extends Controller with OAuth2Provider with LongOrdered {
  implicit val messages : Map[String, String] = Map(
    "error.email-exits" -> "Your email exits"
  )

  def create(target: String, targetId: String) = Action(parse.json(1024 * 1024 * 100)) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body

      val contentReads =
        (__ \ 'body).read[String]


      contentReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { data =>
          Target.getRow(target, targetId).map { row =>
            val comment = Comment.create(data, target, row.getUUID("id"), authInfo.user)
            val response = Json.toJson(comment)
            Ok(response)
          }.getOrElse(NotFound)
        }
      )
    }
  }

  def list(target: String, targetId: String) = Action { implicit request =>
    val id = request.getQueryString("id").map(UUID.fromString(_)).getOrElse(null)
    val before: Double = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

    val next = if(id != null) (before, id) else null

    val key = target + "_" + targetId
    val list = ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
      fa = { error =>
        Target.getRow(target, targetId).map { row =>
          Comment.getBest(key, 10, next)
        }
      },
      fb = { authInfo =>
        Target.getRow(target, targetId).map { row =>
          Comment.getBest(key, 10, next, authInfo.user)
        }
      }
    )

    Ok(Json.toJson(list.getOrElse(List())))
  }
}
