package com.velocidi.apso.json

import scala.collection.JavaConverters._

import io.circe.Json
import io.circe.syntax._
import spray.json.{ enrichAny => _, enrichString => _, _ }

/**
 * Object containing helpers for converting between JSON values and other
 * structures.
 */
object JsonConvert {

  /**
   * Converts an object to a circe JSON value using the most suitable data types.
   * @param obj the object to convert
   * @return the given object converted to a circe JSON value.
   */
  def toCirceJson(obj: Any): Json = obj match {
    case null => Json.Null
    case n: Int => n.asJson
    case n: Long => n.asJson
    case n: Double => n.asJson
    case b: Boolean => b.asJson
    case str: String => str.asJson
    case map: Map[_, _] => Json.obj(map.map { case (k, v) => (k.toString, toCirceJson(v)) }.toList: _*)
    case map: java.util.Map[_, _] => Json.obj(map.asScala.map({ case (k, v) => (k.toString, toCirceJson(v)) }).toList: _*)
    case t: TraversableOnce[_] => Json.fromValues(t.map(toCirceJson).toVector)
    case t: java.lang.Iterable[_] => Json.fromValues(t.asScala.map(toCirceJson).toVector)
    case _ => Json.fromString(obj.toString)
  }

  /**
   * Converts an object to a spray-json JSON value using the most suitable data types.
   * @param obj the object to convert
   * @return the given object converted to a spray-json JSON value.
   */
  def toSprayJson(obj: Any): JsValue = obj match {
    case null => JsNull
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case b: Boolean => JsBoolean(b)
    case str: String => JsString(str)
    case map: Map[_, _] => JsObject(map.map { case (k, v) => (k.toString, toSprayJson(v)) })
    case map: java.util.Map[_, _] => JsObject(map.asScala.map({ case (k, v) => (k.toString, toSprayJson(v)) }).toMap)
    case t: TraversableOnce[_] => JsArray(t.map(toSprayJson).toVector)
    case t: java.lang.Iterable[_] => JsArray(t.asScala.map(toSprayJson).toVector)
    case _ => JsString(obj.toString)
  }
}
