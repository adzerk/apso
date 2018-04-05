package eu.shiftforward.apso.json

import scala.collection.JavaConverters._

import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.syntax._

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
  def toJson(obj: Any): Json = obj match {
    case null => Json.Null
    case str: String => str.asJson
    case n: Int => n.asJson
    case n: Double => n.asJson
    case map: Map[_, _] => Json.fromJsonObject(JsonObject.fromIterable(map.map { case (k, v) => (k.toString, toJson(v)) }))
    case map: java.util.Map[_, _] => Json.fromJsonObject(JsonObject.fromIterable(map.asScala.map({ case (k, v) => (k.toString, toJson(v)) }).toMap))
    case t: TraversableOnce[_] => Json.fromValues(t.map(toJson).toVector)
    case t: java.lang.Iterable[_] => Json.fromValues(t.asScala.map(toJson).toVector)
    case n: Long => n.asJson
    case b: Boolean => b.asJson
    case _ => Json.fromString(obj.toString)
  }
}
