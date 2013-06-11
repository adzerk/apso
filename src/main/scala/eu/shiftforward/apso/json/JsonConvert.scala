package eu.shiftforward.apso.json

import spray.json._

object JsonConvert {
  def toJson(obj: Any): JsValue = obj match {
    case null => JsNull
    case str: String => JsString(str)
    case n: Int => JsNumber(n)
    case n: Double => JsNumber(n)
    case map: Map[_, _] => JsObject(map.map { case (k, v) => (k.toString, toJson(v)) })
    case t: TraversableOnce[_] => JsArray(t.map(toJson(_)).toList)
    case n: Long => JsNumber(n)
    case b: Boolean => JsBoolean(b)
    case _ => JsString(obj.toString)
  }
}
