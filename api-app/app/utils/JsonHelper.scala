package utils

import play.api.libs.json._
import play.api.libs.json.JsObject
import java.util.{Date, UUID}
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

object JsonHelper {
  implicit val cMapWrites: OWrites[Map[String, Any]] = OWrites[Map[String, Any]] { ts =>
    val result = ts.map { item =>
      val v = item._2 match {
        case v: String => Json.toJson(v)
        case v: Int => Json.toJson(v)
        case v: Short => Json.toJson(v)
        case v: Long => Json.toJson(v)
        case v: Double => Json.toJson(v)
        case v: Float => Json.toJson(v)
        case v: BigDecimal => Json.toJson(v)
        case v: Boolean => Json.toJson(v)
        case v: UUID => Json.toJson(v.toString)
        case v: Date => Json.toJson(v)
        case l: List[_] => {
          JsArray(l.map(toJson(_)).toSeq)
        }
        case l: Set[_] => {
          JsArray(l.map(toJson(_)).toSeq)
        }
        case l: ArrayBuffer[_] => {
          JsArray(l.map(toJson(_)).toSeq)
        }
        case m: Map[_, _] => {
          JsObject(
            m.map { item => (item._1.toString, toJson(item._2)) }.toSeq
          )
        }
        case null => JsNull
        case v: java.util.HashMap[String, Object] => Json.toJson(v.toMap)
        case v: java.util.ArrayList[AnyRef] => JsArray(v.toList.map(toJson(_)).toSeq)
        case _ => {
          println("invalid json from cMapWrites:" + item)
          println("invalid json from cMapWrites:" + item._2)
          println("invalid json from cMapWrites:" + item._2.getClass)
          throw new exception.NeedImplement
        }
      }

      (item._1, v)
    }.toSeq
    JsObject(result)
  }


  def toJson(in: Any) = {
    in match {
      case v: String => Json.toJson(v)
      case v: Int => Json.toJson(v)
      case v: Short => Json.toJson(v)
      case v: Long => Json.toJson(v)
      case v: Double => Json.toJson(v)
      case v: Float => Json.toJson(v)
      case v: BigDecimal => Json.toJson(v)
      case v: Boolean => Json.toJson(v)
      case v: UUID => Json.toJson(v.toString)
      case v: Date => Json.toJson(v)
      case v: java.util.HashMap[String, Object] => Json.toJson(v.toMap)(cMapWrites)
      case v: Map[String, _] => Json.toJson(v)(cMapWrites)
      case null => JsNull
      case _ => {
        println("in is:" + in)
        println("in is:" + in.getClass)
        throw new exception.NeedImplement
      }
    }
  }
}
