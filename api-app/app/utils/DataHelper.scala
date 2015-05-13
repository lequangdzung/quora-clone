package utils

import java.util.{UUID, Date}

object DataHelper {
  def anyToObject(in: Any): AnyRef = {
    in match {
      case v: String => v
      case v: Boolean => Boolean.box(v)
      case v: Date => v
      case v: Set[_] => v
      case v: Map[_, _] => v
      case v: UUID => v
      //@todo: need to implement for more scala data type
      case _ => throw new exception.NeedImplement
    }
  }
}
