package validation

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.data.validation.ValidationError

import play.api.mvc._
import play.mvc.Http

import models.v2.StringUnique
object Helper {
  def stringUnique(table: String, field: String)(implicit r: Reads[String]): Reads[String] = {
    Reads.filter(ValidationError("error.unique"))(p => StringUnique.isUnique(table, field, p))
  }
  implicit def error2Json(in: Seq[(JsPath, Seq[ValidationError])])(implicit messages: Map[String, String] = Map(), dataSubmitted: JsValue, desc: String = "")  = {
    val code = Http.Status.BAD_REQUEST

    var errors: JsObject = Json.obj(
      "code" -> code
    )

    if(desc != "") {
      errors = errors + ("description", JsString(desc))
    }

    var meta : JsObject = JsObject(Seq())

    meta = in.foldLeft(meta) { (b, field_err) =>
      val errList = JsArray(
        field_err._2.map { validateError =>
          val errs = validateError.message.split("""\.""")
          val errCode = errs(errs.length - 1)
          val message = messages.get(errCode).getOrElse("")
          Json.obj(
            "code" -> errCode,
            "message" -> message
          ).as[JsValue]
        }.toSeq

      )
      b + (field_err._1.toString().stripPrefix("/"), errList)
    }

    errors = errors + ("meta", meta)
    errors = errors + ("submitted" -> dataSubmitted)

    new Results.Status(code)(errors).withHeaders("Access-Control-Allow-Origin" -> "*")
  }
}
