package eu.shiftforward.apso.json

import spray.json._

object Implicits {

  final implicit class RichJsValue(val jsValue: JsValue) extends AnyVal {
    def toValue: Any = jsValue match {
      case JsString(str) => str
      case JsNumber(num) => num
      case JsObject(map) => map.mapValues(_.toValue).map(identity)
      case JsArray(elems) => elems.map(_.toValue)
      case JsBoolean(bool) => bool
    }
  }
}
