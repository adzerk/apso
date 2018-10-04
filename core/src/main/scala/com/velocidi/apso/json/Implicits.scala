package com.velocidi.apso.json

import scala.util.Try

import io.circe._
import spray.json.DefaultJsonProtocol._
import spray.json._

import com.velocidi.apso.Implicits._

/**
 * Object containing implicit classes and methods related to JSON handling.
 */
object Implicits {

  object ToInt {
    def unapply(str: String) = Try(str.toInt).toOption
  }

  /**
   * Implicit class that provides new methods for `JsValues`.
   * @param json the `JsValue` to which the new methods are provided.
   */
  final implicit class ApsoJsonJsValue(val json: JsValue) extends AnyVal {

    /**
     * Unwraps a JSON value. If the given value is a JSON string, number or
     * boolean, a `String`, `BigDecimal` or `Boolean` is returned
     * respectively. If the given value is a JSON array or object, a `List[Any]`
     * or `Map[String, Any]` is returned respectively, where each of the values
     * is recursively unwrapped. If the given value is a JSON null, `null` is
     * returned.
     * @return the unwrapped JSON value.
     */
    def toValue: Any = json match {
      case JsString(str) => str
      case JsNumber(num) => num
      case JsObject(map) => map.mapValues(_.toValue).map(identity)
      case JsArray(elems) => elems.map(_.toValue)
      case JsBoolean(bool) => bool
      case JsNull => null
    }

    /**
     * Merges two JsValues if they are JsArrays or JsObjects. If they are JsObjects, a deep merge is performed.
     *
     * When merging JsObjects, if `failOnConflict` is false and conflicts exists between terminal values, these are
     * resolved by using the values of the second JSON. If `failOnConflict` is true, an `IllegalArgumentException` is
     * thrown.
     *
     * @param other the other JSON value to merge
     * @param failOnConflict whether to fail or resolve conflicts by using the values on the `other` JSON.
     * @return the resulting merged JsObject
     */
    def merge(other: JsValue, failOnConflict: Boolean = true): JsValue = (json, other) match {
      case (JsObject(fields), JsObject(otherFields)) =>
        fields.twoWayMerge(otherFields)((js1, js2) => js1.merge(js2, failOnConflict)).toJson
      case (JsArray(arr), JsArray(otherArr)) => (arr ++ otherArr).toJson
      case (_, anyVal) if !failOnConflict => anyVal
      case _ => throw new IllegalArgumentException("Invalid types for merging")
    }
  }

  /**
   * Implicit class that provides new methods for `JsObjects`.
   * @param json the `JsObjects` to which the new methods are provided.
   */
  final implicit class ApsoJsonJsObject(val json: JsObject) extends AnyVal {
    /**
     * Returns a set of keys-value pairs of this object where nested keys are separated by a separator character.
     *
     * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".") = Set("a" -> JsNumber(1),"b.c" -> JsNumber(2), "d" -> JsNull)
     *
     * @param separator character separator to use
     * @return flattened key set
     */
    def flattenedKeyValueSet(separator: String = "."): Set[(String, JsValue)] = {
      val fields = json.fields.toSet
      fields.flatMap {
        case (k, v: JsObject) => v.flattenedKeyValueSet(separator).map { case (kk, vv) => (k + separator + kk) -> vv }
        case (k, v) => Set(k -> v)
      }
    }

    /**
     * Returns a set of keys of this object where nested keys are separated by a separator character.
     *
     * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".", ignoreNull = true) = Set("a","b.c")
     *
     * @param separator character separator to use
     * @param ignoreNull if set, fields with a null value are ignored
     * @return flattened key set
     */
    def flattenedKeySet(separator: String = ".", ignoreNull: Boolean = true): Set[String] =
      flattenedKeyValueSet(separator).filter {
        case (_, JsNull) => !ignoreNull
        case _ => true
      }.map(_._1)
  }

  final implicit class ApsoJsonObject(val json: Json) extends AnyVal {

    /**
     * Returns a set of keys-value pairs of this object where nested keys are separated by a separator character.
     *
     * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".") = Set("a" -> JNumber(1),"b.c" -> JNumber(2), "d" -> JNull)
     *
     * @param separator character separator to use
     * @return flattened key set
     */
    def flattenedKeyValueSet(separator: String = "."): Set[(String, Json)] = {
      json.asObject match {
        case None => Set.empty
        case Some(jo) =>
          val fields = jo.toMap.toSet
          fields.flatMap {
            case (k, v) if v.isObject => v.flattenedKeyValueSet(separator).map { case (kk, vv) => (k + separator + kk) -> vv }
            case (k, v) => Set(k -> v)
          }
      }
    }

    /**
     * Returns a set of keys of this object where nested keys are separated by a separator character.
     *
     * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".", ignoreNull = true) = Set("a","b.c")
     *
     * @param separator character separator to use
     * @param ignoreNull if set, fields with a null value are ignored
     * @return flattened key set
     */
    def flattenedKeySet(separator: String = ".", ignoreNull: Boolean = true): Set[String] =
      flattenedKeyValueSet(separator).filter {
        case (_, v) => !ignoreNull || !v.isNull
      }.map(_._1)

    /**
     * Returns the value of the field on the end of the tree, separated by the separator character.
     *
     * Eg. {"a":{"b":1}}.getField("a.b") = 1
     *
     * @param fieldPath path from the root of the json object to the field
     * @param separator character that separates each element of the path
     * @tparam A type of the field value
     * @return an option with the field value
     */
    def getField[A: Decoder](fieldPath: String, separator: Char = '.'): Option[A] =
      getCursor(fieldPath, separator).as[A].fold(_ => None, Some(_))

    /**
     * Delete a field on a json object.
     *
     * Eg. {"a":1,"b":{"c":2},"d":null}.deleteField("b.c") = {"a":1,"b":{},"d":null}
     *
     * @param fieldPath path from the root of the json object to the field
     * @param separator character that separates each element of the path
     * @return the json without the deleted value
     */
    def deleteField(fieldPath: String, separator: Char = '.'): Json = {
      require(fieldPath.nonEmpty, "The field path must have value.")

      getCursor(fieldPath, separator).delete.top.get
    }

    /**
     * Returns a cursor on the field on the end of the tree, separated by the separator character.
     *
     * @param fieldPath path from the root of the json object to the field
     * @param separator character that separates each element of the path
     * @return cursor to the field value
     */
    def getCursor(fieldPath: String, separator: Char): ACursor =
      fieldPath.split(separator)
        .foldLeft(json.hcursor: ACursor) {
          case (cursor, field) =>
            cursor.downField(field)
        }
  }

  /**
   * Creates a JsObject from a sequence of pairs of dot-separated (or other separator) paths with the corresponding
   * leaf values (eg. `List(("root.leaf1", JsString("leafVal1")), ("root.leaf2", JsString("leafVal2")))`
   * @param paths the sequence of dot-separated (or other separator) paths
   * @param separatorRegex regex to use to separate fields
   * @return the resulting JsObject
   */
  def fromFullPaths(paths: Seq[(String, JsValue)], separatorRegex: String = "\\."): JsValue = {
    def createJsValue(keys: Seq[String], value: JsValue): JsValue = {
      keys match {
        case Nil => value
        case h :: t => JsObject(h -> createJsValue(t, value))
      }
    }

    paths match {
      case Nil => JsObject()
      case (path, value) :: rem =>
        createJsValue(path.split(separatorRegex).toList, value).merge(fromFullPaths(rem, separatorRegex))
    }
  }

  /**
   * Creates a Json from a sequence of pairs of dot-separated (or other separator) paths with the corresponding
   * leaf values (eg. `List(("root.leaf1", "leafVal1"), ("root.leaf2", "leafVal2"))`
   * @param paths the sequence of dot-separated (or other separator) paths
   * @param separatorRegex regex to use to separate fields
   * @return the resulting Json object
   */
  def fromCirceFullPaths(paths: Seq[(String, Json)], separatorRegex: String = "\\."): Json = {
    def createJson(keys: Seq[String], value: Json): Json = {
      keys match {
        case Nil => value
        case h :: t => Json.obj(h -> createJson(t, value))
      }
    }

    paths match {
      case Nil => Json.obj()
      case (path, value) :: rem =>
        createJson(path.split(separatorRegex).toList, value).deepMerge(fromCirceFullPaths(rem, separatorRegex))
    }
  }
}
