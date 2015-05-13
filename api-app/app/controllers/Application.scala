package controllers

import play.api._
import play.api.mvc._

import java.util.{UUID, Date}
import scala.collection.JavaConversions._

import com.datastax.driver.core.querybuilder.QueryBuilder
import database.Cassandra

import scalaoauth2.provider._
import security.oauth2.CustomDataHandler

object Application extends Controller with OAuth2Provider {
  def index = Action { implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }

  def options(path: String) = Action { implicit request =>
    Ok.withHeaders("Access-Control-Allow-Origin" -> "*", "Access-Control-Allow-Methods" -> "GET, PUT, POST, DELETE, PACTH, OPTIONS", "Access-Control-Allow-Headers" -> "accept, origin, Content-type, x-json, x-prototype-version, x-requested-with")
  }
}