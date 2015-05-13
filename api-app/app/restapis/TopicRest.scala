package restapis

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

import scalaoauth2.provider._
import security.oauth2.CustomDataHandler

import models.{Question, Topic}
import utils.JsonHelper._
import validation.Helper._
import utils.{PrefixHelper, Alias, KeyHelper}


object TopicRest extends Controller with OAuth2Provider {
  def create = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body
      val topicReads =  (__ \ 'name).read[String]

      topicReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { name =>
          val result = Topic.create(name, authInfo.user)
          Ok(
            Json.toJson(Map(
              "id" -> result._1,
              "name" -> result._2,
              "status" -> result._3
            ))
          )
        }
      )
    }
  }

  def search = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body
      val searchReads =
        (__ \ 'name).read[String]  and
        (__ \ 'ids).readNullable[Set[UUID]] tupled

      searchReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { data =>
          val result = Topic.search(data._1, data._2.getOrElse(Set[UUID]()))
          Ok(Json.toJson(
            Map(
              "topics" -> result._1.toList,
              "found" -> result._2
            )
          ))
        }
      )
    }
  }

  def getQuestions(alias: String) = Action { implicit request =>
    Alias.get(Alias.TOPIC_KEY, alias).map { data =>
      val id = UUID.fromString(data("id"))
      val topic = Topic.getRow(data("id")).get

      val limit = request.getQueryString("count").map(_.toInt).getOrElse(constants.StaticNumber.CONTENT_PER_PAGE)
      val filter = request.getQueryString("filter").getOrElse("hot")

      val before: Long = request.getQueryString("before").map(_.toLong).getOrElse(0)

      val next = if(id != null && before != 0) (before, id) else null
      val key = PrefixHelper.getTopicKey(id)

      ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
        fa = { error =>
          val questions = filter match {
            case "new" => Question.getNewest(key, limit, next)
            case "best" => {
              val before = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

              val next = if(id != null) (before, id) else null
              Question.getBestAnswer(key, limit, next)
            }
            case _ => Question.getHot(key, limit, next)
          }

          Ok(
            Json.obj(
              "topic" -> Json.obj(
                "name" -> topic.getString("name"),
                "description" -> topic.getString("description"),
                "alias" -> topic.getString("alias"),
                "followerCount" -> topic.getLong("follower_count"),
                "questionCount" -> topic.getLong("question_count"),
                "status" -> topic.getBool("status"),
                "updated" -> topic.getDate("updated"),
                "thumb" -> topic.getString("thumb")
              ),
              "questions" -> Json.toJson(questions)
            )
          )
        },
        fb = { authInfo =>
          val questions = filter match {
            case "new" => Question.getNewest(key, limit, next, authInfo.user)
            case "best" => {
              val before = request.getQueryString("before").map(_.toDouble).getOrElse(0D)

              val next = if(id != null) (before, id) else null
              Question.getBestAnswer(key, limit, next, authInfo.user)
            }
            case _ => Question.getHot(key, limit, next, authInfo.user)
          }
          Ok(Json.obj(
            "topic" -> Json.obj(
              "name" -> topic.getString("name"),
              "description" -> topic.getString("description"),
              "alias" -> topic.getString("alias"),
              "followerCount" -> topic.getLong("follower_count"),
              "questionCount" -> topic.getLong("question_count"),
              "status" -> topic.getBool("status"),
              "updated" -> topic.getDate("updated"),
              "thumb" -> topic.getString("thumb")
            ),
            "questions" -> Json.toJson(questions)
          ))
        }
      )
    }.getOrElse(NotFound("cannot found this topic"))

  }
}
