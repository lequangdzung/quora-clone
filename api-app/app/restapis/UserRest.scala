package restapis


import play.api._
import play.api.mvc._

import java.util.{Date, UUID}
import scala.collection.JavaConversions._

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Reads._
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import validation.Helper._
import models._

import scalaoauth2.provider._
import security.oauth2.{TokenStorage, CustomDataHandler}
import java.util.UUID
import constants.PubSub._
import play.api.data.validation.ValidationError

import utils.JsonHelper._
import play.api.data.validation.ValidationError
import play.api.libs.iteratee.{Enumerator, Concurrent, Iteratee}
import scala.concurrent.Future
import utils.{PrefixHelper, Alias}
import pubsub.{NotifyData, PubSubHelper}

object UserRest extends Controller with OAuth2Provider {
  implicit val messages : Map[String, String] = Map(
    "error.email-exits" -> "Email này đã tồn tại"
  )

  def create = Action(parse.json) { implicit request =>
    implicit val dataSubmitted = request.body
    val userReads = (
      (__ \ 'email).read[String](Reads.email andKeep stringUnique("user", "email")) and
      (__ \ 'password).read[String](minLength[String](4)) and
      (__ \ 'display).read[String]
    ) tupled

    userReads.reads(dataSubmitted).fold(
      invalid = { errors =>
        errors
      },
      valid = { data =>
        val user = models.User.addUser(data._1, data._2, data._3)
        val accessToken = TokenStorage.createAccessTokenFromUser(user)

        Ok(Json.obj(
          "access_token" -> accessToken.token,
          "refresh_token" -> accessToken.refreshToken.get,
          "user" -> Json.obj(
            "display" -> user.display,
            "avatar" -> user.avatar,
            "id" -> user.id,
            "email" -> user.email,
            "followerCount" -> user.followerCount,
            "unread" -> user.unread
          )
        ))
      }
    )
  }

  def getQuestions(alias: String) = Action { implicit request =>
    Alias.get(Alias.USER_KEY, alias).map { data =>
      val id = UUID.fromString(data("id"))
      val user = User.getRow(data("id")).get

      val limit = request.getQueryString("count").map(_.toInt).getOrElse(constants.StaticNumber.CONTENT_PER_PAGE)
      val filter = request.getQueryString("filter").getOrElse("hot")

      val before: Long = request.getQueryString("before").map(_.toLong).getOrElse(0)

      val next = if(id != null && before != 0) (before, id) else null
      val key = PrefixHelper.getUserKey(id)

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
              "user" -> Json.obj(
                "display" -> user.getString("display"),
                "alias" -> user.getString("alias"),
                "followerCount" -> user.getLong("follower_count"),
                "updated" -> user.getDate("created"),
                "avatar" -> user.getString("avatar")
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
            "user" -> Json.obj(
              "display" -> user.getString("display"),
              "alias" -> user.getString("alias"),
              "followerCount" -> user.getLong("follower_count"),
              "updated" -> user.getDate("created"),
              "avatar" -> user.getString("avatar")
            ),
            "questions" -> Json.toJson(questions)
          ))
        }
      )
    }.getOrElse(NotFound("cannot found this topic"))

  }

  def getOwnerQuestions = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val limit = request.getQueryString("count").map(_.toInt).getOrElse(constants.StaticNumber.CONTENT_PER_PAGE)
      val filter = request.getQueryString("filter").getOrElse("hot")

      val before: Long = request.getQueryString("before").map(_.toLong).getOrElse(0)

      val id = authInfo.user.id
      val key = PrefixHelper.getUserKey(id)
      val next = if(before != 0) (before, id) else null

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
        "user" -> Json.obj(
          "display" -> authInfo.user.display,
          "avatar" -> authInfo.user.avatar
        ),
        "questions" -> Json.toJson(questions)
      ))
    }
  }

  def getFollowTopics = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val result = User.getFollowTopics(authInfo.user)
      Ok(Json.toJson(result))
    }
  }

  def getFollowUsers = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val result = User.getFollowUsers(authInfo.user)
      Ok(Json.toJson(result))
    }
  }

  def getFollowingUsers = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val result = User.getFollowingUsers(authInfo.user)
      Ok(Json.toJson(result))
    }
  }

  def updatePassword = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body
      val passwordReads = (
        (__ \ 'password).read[String](minLength[String](4))
        )

      passwordReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { password =>
          User.updatePassword(authInfo.user, password)
          Ok
        }
      )
    }
  }

  def getNotifications = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val unreadNumber = authInfo.user.unread

      val notifications = Notify.getNotifications(authInfo.user.id, unreadNumber)

      // Token need to reset number unread

      Ok(Json.toJson(notifications))
    }
  }

  def getUnread = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      val unreadNumber = authInfo.user.unread
      Ok(
        Json.obj("unread" -> unreadNumber)
      )
    }
  }

  def followTopic(topicId: String) = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Topic.getRow(topicId).map { row =>
        val about = request.getQueryString("about").getOrElse("follow")
        val result = User.followTopic(authInfo.user, row.getUUID("id"), about)
        if(result) Ok
        else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def followTopics = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body
      val topicsReads = (
        (__ \ 'ids).read[Set[UUID]]
        )

      topicsReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { ids =>
          Topic.getRows(ids).map { row =>
            val about = request.getQueryString("about").getOrElse("follow")
            User.followTopic(authInfo.user, row.getUUID("id"), about)
          }

          Ok
        }
      )
    }
  }

  def followQuestion(questionId: String) = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Question.getRow(questionId).map { row =>
        val result = User.followQuestion(authInfo.user, row.getUUID("id"), row.getLong("score"), row.getDate("updated"), row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        if(result) Ok
        else BadRequest("invalid action")
      }.getOrElse(NotFound)
    }
  }

  def followUser(userId: String) = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      if(authInfo.user.id.toString == userId) {
        Locked("Bạn không thể theo dõi chính bạn")
      }
      else {
        User.getRow(userId).map { row =>
          val result = User.followUser(authInfo.user, row.getUUID("id"))
          if(result) Ok
          else BadRequest
        }.getOrElse(NotFound("cannot found user with id: " + userId))
      }
    }
  }

  def unFollowTopic(topicId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Topic.getRow(topicId).map { row =>
        val result = User.unFollowTopic(authInfo.user, row.getUUID("id"))
        if(result) NoContent
        else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def unFollowQuestion(questionId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Question.getRow(questionId).map { row =>
        val result = User.unFollowQuestion(authInfo.user, row.getUUID("id"), row.getLong("score"), row.getDate("updated"), row.getUUID("creator_id"), row.getList("topics", classOf[UUID]).toList)
        if(result) NoContent
        else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def unFollowUser(userId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      User.getRow(userId).map { row =>
        val result = User.unFollowUser(authInfo.user, row.getUUID("id"))
        if(result) NoContent
        else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def voteUp(target: String, targetId: String) = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Answer.getRow(targetId).map { row =>
        val result = User.voteUp(authInfo.user, target, row.getUUID("id"), row.getLong("vote_up"), row.getLong("vote_down"), row.getLong("score"), row.getDouble("confidence"), row.getDate("updated"), "question", row.getUUID("question_id"))
        if(result) {
          PubSubHelper.publish(TOPIC_NOTIFY, NotifyData("voteup", target, row.getUUID("id"), authInfo.user.id, "", ""))
          Ok
        } else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def deleteVoteUp(target: String, targetId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Answer.getRow(targetId).map { row =>
        val result = User.deleteVoteUp(authInfo.user, target, row.getUUID("id"), row.getLong("vote_up"), row.getLong("vote_down"), row.getLong("score"), row.getDouble("confidence"), row.getDate("updated"), "question", row.getUUID("question_id"))
        if(result) Ok else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def voteDown(target: String, targetId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Answer.getRow(targetId).map { row =>
        val result = User.voteDown(authInfo.user, target, row.getUUID("id"), row.getLong("vote_up"), row.getLong("vote_down"), row.getLong("score"), row.getDouble("confidence"), row.getDate("updated"), "question", row.getUUID("question_id"))
        if(result) Ok else BadRequest
      }.getOrElse(NotFound)
    }
  }

  def deleteVoteDown(target: String, targetId: String) = Action { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      Answer.getRow(targetId).map { row =>
        val result = User.deleteVoteDown(authInfo.user, target, row.getUUID("id"), row.getLong("vote_up"), row.getLong("vote_down"), row.getLong("score"), row.getDouble("confidence"), row.getDate("updated"), "question", row.getUUID("question_id"))
        if(result) Ok else BadRequest
      }.getOrElse(NotFound)
    }
  }
}
