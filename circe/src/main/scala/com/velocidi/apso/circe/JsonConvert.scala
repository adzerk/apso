package com.velocidi.apso.circe

import scala.collection.compat._
import scala.jdk.CollectionConverters._

import io.circe.Json
import io.circe.syntax._

/** Object containing helpers for converting between JSON values and other structures.
  */
object JsonConvert {

  /** Converts an object to a [[io.circe.Json]] value using the most suitable data types.
    * @param obj the object to convert
    * @return the given object converted to a [[io.circe.Json]] value.
    */
  def toJson(obj: Any): Json = obj match {
    case null           => Json.Null
    case n: Int         => n.asJson
    case n: Long        => n.asJson
    case n: Double      => n.asJson
    case b: Boolean     => b.asJson
    case str: String    => str.asJson
    case map: Map[_, _] => Json.obj(map.map { case (k, v) => (k.toString, toJson(v)) }.toList: _*)
    case map: java.util.Map[_, _] =>
      Json.obj(map.asScala.map({ case (k, v) => (k.toString, toJson(v)) }).toList: _*)
    case t: IterableOnce[_]       => Json.fromValues(t.iterator.map(toJson).to(Vector))
    case t: java.lang.Iterable[_] => Json.fromValues(t.asScala.map(toJson).toVector)
    case arr: Array[_]            => Json.fromValues(arr.toVector.map(toJson))
    case _                        => Json.fromString(obj.toString)
  }
}
