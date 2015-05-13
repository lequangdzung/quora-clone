package restapis

import java.util.UUID
import scala.collection.JavaConversions._

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
import models.v2.{Token}
import utils.JsonHelper._
import validation.Helper._
import utils.{KeyHelper, Alias}

import elasticsearch.{ElasticSearchTag, ElasticSearchComment, ElasticSearchContent}

object AuthStatus extends Enumeration {
  type AuthStatus = Value

  val InvalidToken, HubNotFound, TokenNotFound, UnAuth, Valid = Value
}


object AdminRest extends Controller with OAuth2Provider {
  import AuthStatus._

  def getContents = Action { implicit request =>
    val start = request.getQueryString("start").map(_.toInt).getOrElse(0)
    val size = request.getQueryString("size").map(_.toInt).getOrElse(5)
    val sort = request.getQueryString("sort").getOrElse("title")
    val sortOrder = request.getQueryString("sortOrder").getOrElse("ASC")
    val query = request.getQueryString("query").getOrElse(null)
    val field = request.getQueryString("field").getOrElse(null)
    val fieldValue = request.getQueryString("fieldValue").getOrElse(null)


    Ok
  }

  def getComments = Action { implicit request =>
    val start = request.getQueryString("start").map(_.toInt).getOrElse(0)
    val size = request.getQueryString("size").map(_.toInt).getOrElse(5)
    val sort = request.getQueryString("sort").getOrElse("user_id")
    val sortOrder = request.getQueryString("order").getOrElse("ASC")
    val query = request.getQueryString("query").getOrElse(null)

    ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
      fa = { error =>
        Unauthorized
      },
      fb = { authInfo =>
        val searchResult = ElasticSearchComment.getComments(query, start, size, sort, sortOrder)
        Ok(Json.obj(
          "total" -> searchResult._1,
          "items" -> searchResult._2
        ))
      }
    )
  }

  def getTags = Action { implicit request =>
    val start = request.getQueryString("start").map(_.toInt).getOrElse(0)
    val size = request.getQueryString("size").map(_.toInt).getOrElse(5)
    val sort = request.getQueryString("sort").getOrElse("name")
    val sortOrder = request.getQueryString("order").getOrElse("ASC")
    val query = request.getQueryString("query").getOrElse(null)

    ProtectedResource.handleRequest(request, new CustomDataHandler()).fold(
      fa = { error =>
        Unauthorized
      },
      fb = { authInfo =>
        val searchResult = ElasticSearchTag.getTags(query, start, size, sort, sortOrder)
        Ok(Json.obj(
          "total" -> searchResult._1,
          "items" -> searchResult._2
        ))
      }
    )
  }

  def createTag = Action(parse.json) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      implicit val dataSubmitted = request.body
      val objectReads = (
        (__ \ 'name).read[String] and
          (__ \ 'description).readNullable[String]
        ) tupled

      objectReads.reads(dataSubmitted).fold(
        invalid = { errors =>
          errors
        },
        valid = { data =>
          Ok
        }
      )
    }
  }
}
