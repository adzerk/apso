package eu.shiftforward.apso.json

import spray.json.DefaultJsonProtocol._
import spray.json._
import eu.shiftforward.apso.Implicits._

import scala.reflect.ClassTag
import scala.util.Try

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
     * Get's a field from a dot-separated path (eg. `root.node.leaf`)
     *
     * @param path The dot separated path
     * @tparam JV The type of JSON value to return
     * @return The JSON field value (wrapped in an option)
     */
    @deprecated("\nPlease check out jrudolph/json-lenses project on GitHub (https://github.com/jrudolph/json-lenses) " +
      "for alternative ways to manipulate JSON values.\n" +
      "For example:\n" +
      "val json = \"\"\"{\"k1\":{\"k1.1\":\"v1.1\",\"k1.2\":\"v1.2\",\"k1.3\":[{\"k1.3.1\":[1,2,3]},{\"k1.3.1\":[4,5,6]}]}}\"\"\"\n" +
      "val lens = \"k1\" / \"k1.3\" / * / \"k1.3.1\"\n" +
      "json.extract[Array[Int]](lens2)\n" +
      "res2: Seq[Array[Int]] = List(Array(1, 2, 3), Array(4, 5, 6))\n", "28-07-2015")
    def getPath[JV <: JsValue: ClassTag](path: String): Option[JV] =
      getPath(path.split("\\.").toList)

    @deprecated("\nPlease check out jrudolph/json-lenses project on GitHub (https://github.com/jrudolph/json-lenses) " +
      "for alternative ways to manipulate JSON values.\n" +
      "For example:\n" +
      "val json = \"\"\"{\"k1\":{\"k1.1\":\"v1.1\",\"k1.2\":\"v1.2\",\"k1.3\":[{\"k1.3.1\":[1,2,3]},{\"k1.3.1\":[4,5,6]}]}}\"\"\"\n" +
      "val lens = \"k1\" / \"k1.3\" / * / \"k1.3.1\"\n" +
      "json.extract[Array[Int]](lens2)\n" +
      "res2: Seq[Array[Int]] = List(Array(1, 2, 3), Array(4, 5, 6))\n", "28-07-2015")
    def getPath[JV <: JsValue: ClassTag](pathElements: List[String]): Option[JV] =
      (pathElements, json) match {
        case (Nil, jv: JV) => Some(jv)
        case (elem :: rem, JsObject(fields)) => fields.get(elem).flatMap(_.getPath(rem))
        case (ToInt(i) :: rem, JsArray(elems)) => elems.drop(i).headOption.flatMap(_.getPath(rem))
        case _ => None
      }

    /**
     * Merge two JsValues if they are JsArrays or JsObjects. If they are JsObjects, a deep merge is performed.
     * @param other the other JsValue
     * @return the merged JsValues
     */
    def merge(other: JsValue): JsValue = (json, other) match {
      case (JsObject(fields), JsObject(otherFields)) =>
        (fields.twoWayMerge(otherFields))((js1, js2) => js1.merge(js2)).toJson
      case (JsArray(arr), JsArray(otherArr)) => (arr ++ otherArr).toJson
      case _ => throw new IllegalArgumentException("Invalid types for merging")
    }
  }

  /**
   * Creates a JsObject from a sequence of pairs of dot-separated paths with the corresponding
   * leaf values (eg. `List(("root.leaf1", JsString("leafVal1")), ("root.leaf2", JsString("leafVal2")))`
   * @param paths the sequence of dot-separated paths
   * @return the resulting JsObject
   */
  def fromFullPaths(paths: Seq[(String, JsValue)]): JsValue = {
    def createJsValue(keys: Seq[String], value: JsValue): JsValue = {
      keys match {
        case Nil => value
        case h :: t => JsObject(h -> createJsValue(t, value))
      }
    }

    paths match {
      case Nil => JsObject()
      case (path, value) :: rem =>
        createJsValue(path.split("\\.").toList, value).merge(fromFullPaths(rem))
    }
  }
}
