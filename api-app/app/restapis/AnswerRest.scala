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

import constants.PubSub._
import models._
import utils.JsonHelper._
import validation.Helper._
import utils.{KeyHelper, Alias}
import pubsub.{NotifyData, PubSubHelper}

object AnswerRest extends Controller with OAuth2Provider with LongOrdered {
  implicit val messages : Map[String, String] = Map(
    "error.email-exits" -> "Your email exits"
  )

  def create(questionId: String) = Action(parse.json(1024 * 1024 * 100)) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body

      val contentReads =
        (__ \ 'body).read[String]


      contentReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { data =>
          Question.getRow(questionId).map { row =>
            val answer = Answer.create(data, row.getUUID("id"), authInfo.user)
            PubSubHelper.publish(TOPIC_NOTIFY, NotifyData("answer", "question", row.getUUID("id"), authInfo.user.id, row.getString("title"), "questions/" + row.getString("alias")))
            val response = Json.toJson(answer)
            Ok(response)
          }.getOrElse(NotFound)
        }
      )
    }
  }

  def list = Action { implicit request =>
    val limit = request.getQueryString("count").map(_.toInt).getOrElse(constants.StaticNumber.CONTENT_PER_PAGE)
    val filter = request.getQueryString("filter").getOrElse("hot")

    val id = request.getQueryString("id").map(UUID.fromString(_)).getOrElse(null)
    val before: Long = request.getQueryString("before").map(_.toLong).getOrElse(0)

    val next = if(id != null && before != 0) (before, id) else null

    ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
      fa = { error =>
        Unauthorized
      },
      fb = { authInfo =>
        val key = KeyHelper.userContents(authInfo.user.id)
        val questions = filter match {
          case "top" => Question.getHot(key, limit, next, authInfo.user)
          case _ => Question.getHot(key, limit, next, authInfo.user)
        }
        Ok(Json.toJson(questions))
      }
    )
  }
}
