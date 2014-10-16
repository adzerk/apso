package eu.shiftforward.apso.json

import scala.collection.JavaConverters._
import spray.json._

/**
 * Object containing helpers for converting between JSON values and other
 * structures.
 */
object JsonConvert {

  /**
   * Converts an object to a JSON value using the most suitable data types.
   * @param obj the object to convert
   * @return the given object converted to a JSON value.
   */
  def toJson(obj: Any): JsValue = obj match {
    case null => JsNull
    case str: String => JsString(str)
    case n: Int => JsNumber(n)
    case n: Double => JsNumber(n)
    case map: Map[_, _] => JsObject(map.map { case (k, v) => (k.toString, toJson(v)) })
    case map: java.util.Map[_, _] => JsObject(map.asScala.map({ case (k, v) => (k.toString, toJson(v)) }).toMap)
    case t: TraversableOnce[_] => JsArray(t.map(toJson(_)).toList)
    case t: java.lang.Iterable[_] => JsArray(t.asScala.map(toJson(_)).toList)
    case n: Long => JsNumber(n)
    case b: Boolean => JsBoolean(b)
    case _ => JsString(obj.toString)
  }
}
