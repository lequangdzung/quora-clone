package controllers

import play.api.mvc._


import scalaoauth2.provider._
import security.oauth2.CustomDataHandler
import java.io.File
import org.apache.commons.io.FilenameUtils
import utils.{MediaUtil, StringHelper}
import utils.JsonHelper._
import play.api.libs.json.Json

object Media extends Controller with OAuth2Provider {

  def upload = Action(parse.multipartFormData) { implicit request =>
    authorize(new CustomDataHandler()) { authInfo =>
      println("request is: " + request)
      request.body.file("file").map { picture =>
        val filename = authInfo.user.id.toString + "_" + picture.filename
        val orgFilename = StringHelper.genUniqueFilename(authInfo.user.id, picture.filename) + "." + FilenameUtils.getExtension(picture.filename)
        val orgFile = new File(s"public/images/$orgFilename")

        picture.ref.moveTo(orgFile)

        Ok(Json.toJson(MediaUtil.renderFitImage(orgFile, FilenameUtils.getBaseName(orgFilename), FilenameUtils.getExtension(orgFilename))))
      }.getOrElse {
        BadRequest
      }
    }
  }
}