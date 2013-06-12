package eu.shiftforward.apso.json

import eu.shiftforward.apso.collection.HMapKey
import spray.json._

abstract class JsonHMapKey[V](val sym: Symbol)(implicit val reg: JsonKeyRegistry, val jsonFormat: JsonFormat[V]) extends HMapKey[V] {
  reg.keys += (sym -> this)

  override def toKey: JsonHMapKey[Value] = this
  def toJson(v: Value): JsValue = jsonFormat.write(v)
  def toValue(v: JsValue): Value = jsonFormat.read(v)
}
