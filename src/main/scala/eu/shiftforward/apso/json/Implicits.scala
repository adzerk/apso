package eu.shiftforward.apso.json

import spray.json._

/**
 * Object containing implicit classes and methods related to JSON handling.
 */
object Implicits {

  /**
   * Implicit class that provides new methods for `JsValues`.
   * @param jsValue the `JsValue` to which the new methods are provided.
   */
  final implicit class ApsoJsonJsValue(val jsValue: JsValue) extends AnyVal {

    /**
     * Unwraps a JSON value. If the given value is a JSON string, number or
     * boolean, a `String`, `BigDecimal` or `Boolean` is returned
     * respectively. If the given value is a JSON array or object, a `List[Any]`
     * or `Map[String, Any]` is returned respectively, where each of the values
     * is recursively unwrapped. If the given value is a JSON null, `null` is
     * returned.
     * @return the unwrapped JSON value.
     */
    def toValue: Any = jsValue match {
      case JsString(str) => str
      case JsNumber(num) => num
      case JsObject(map) => map.mapValues(_.toValue).map(identity)
      case JsArray(elems) => elems.map(_.toValue)
      case JsBoolean(bool) => bool
      case JsNull => null
    }
  }
}
