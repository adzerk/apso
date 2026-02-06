package com.kevel.apso.circe

import scala.jdk.CollectionConverters.*

import io.circe.syntax.*
import io.circe.{Json, JsonObject}

/** Object containing helpers for converting between JSON values and other structures.
  */
object JsonConvert {

  /** Converts an object to a [[io.circe.Json]] value using the most suitable data types.
    * @param obj
    *   the object to convert
    * @return
    *   the given object converted to a [[io.circe.Json]] value.
    */
  def toJson(obj: Any): Json = obj match {
    case null           => Json.Null
    case n: Int         => n.asJson
    case n: Long        => n.asJson
    case n: Double      => n.asJson
    case b: Boolean     => b.asJson
    case str: String    => str.asJson
    case map: Map[?, ?] =>
      Json.fromJsonObject(
        JsonObject.fromIterable(map.iterator.map { case (k, v) => (k.toString, toJson(v)) }.to(Iterable))
      )
    case map: java.util.Map[?, ?] =>
      Json.fromJsonObject(
        JsonObject.fromIterable(map.asScala.iterator.map { case (k, v) => (k.toString, toJson(v)) }.to(Iterable))
      )
    case t: IterableOnce[?]       => Json.fromValues(t.iterator.map(toJson).toVector)
    case t: java.lang.Iterable[?] => Json.fromValues(t.asScala.map(toJson).toVector)
    case arr: Array[?]            => Json.fromValues(arr.toVector.map(toJson))
    case _                        => Json.fromString(obj.toString)
  }
}
