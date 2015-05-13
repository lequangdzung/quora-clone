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

import models._
import utils.JsonHelper._
import validation.Helper._
import utils.{KeyHelper, Alias}

import play.api.data.validation.ValidationError
import constants.StaticNumber

object QuestionRest extends Controller with OAuth2Provider with LongOrdered {
  implicit val messages : Map[String, String] = Map(
    "error.email-exits" -> "Your email exits"
  )

  def tagValid(implicit r: Reads[List[Map[String, String]]]): Reads[List[Map[String, String]]] =
    Reads.filterNot(ValidationError("error.tagValid"))(p => !(p.size > 0 && p.size < 6))

  def create = Action(parse.json(1024 * 1024 * 100)) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body

      val contentReads = (
        (__ \ 'title).read[String] and
          (__ \ 'topics).read[List[Map[String, String]]](tagValid)
        ) tupled

      contentReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { data =>
          try {
            val question = Question.create(data._1, null, null, data._2, authInfo.user)
            Ok(Json.toJson(question))
          }
          catch {
            case e: exception.BadData => {
              BadRequest(e.field)
            }
          }
        }
      )
    }
  }

  def confirm = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body

      val questionReads = (
        (__ \ 'title).read[String]
        )

      questionReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { title =>
          val result = Question.confirm(title)

          Ok(Json.toJson(result))
        }
      )
    }
  }

  def search = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body

      val query = request.getQueryString("query").getOrElse("")
      val result = Question.search(query)

      Ok(Json.toJson(result))
    }
  }


  def get(alias: String) = Action { implicit request =>
    Alias.get(Alias.QUESTION_KEY, alias).map { data =>
      val id = UUID.fromString(data("id"))
      val question = ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
        fa = { error =>
          Question.get(id)
        },
        fb = { authInfo =>
          Question.get(id, authInfo.user)
        }
      )

      Ok(Json.obj("question" -> Json.toJson(question)))
    }.getOrElse(NotFound("cannot found the question"))
  }

  def getAnswers(questionId: String) = Action { implicit request =>
    val filter = request.getQueryString("filter").getOrElse("hot")

    val id = request.getQueryString("id").map(UUID.fromString(_)).getOrElse(null)
    val before = request.getQueryString("before").map(_.toLong).getOrElse(0L)

    val next = if(id != null && before.toString != 0) (before, id) else null
    Question.getRow(questionId).map { row =>
      val questionId = row.getUUID("id")
      val answers = ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
        fa = { error =>
          if(filter == "top") Answer.getTop("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next)
          else if(filter == "new") Answer.getNewest("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next)
          else {
            val before = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

            val next = if(id != null) (before, id) else null
            Answer.getHot("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next)
          }
        },
        fb = { authInfo =>
          if(filter == "top") Answer.getTop("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next, authInfo.user)
          else if(filter == "new") Answer.getNewest("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next, authInfo.user)
          else {
            val before = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

            val next = if(id != null) (before, id) else null

            Answer.getHot("question_" + questionId.toString, StaticNumber.ANSWER_PER_PAGE, next, authInfo.user)
          }
        }
      )

      Ok(Json.toJson(answers))
    }.getOrElse(NotFound("cannot found the question"))
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
          case "new" => Question.getNewest(key, limit, next, authInfo.user)
          case "best" => {
            val before = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

            val next = if(id != null) (before, id) else null
            Question.getBestAnswer(key, limit, next, authInfo.user)
          }
          case _ => Question.getHot(key, limit, next, authInfo.user)
        }
        Ok(Json.toJson(questions))
      }
    )
  }
}
