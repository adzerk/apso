package eu.shiftforward.apso.json

import eu.shiftforward.apso.collection.HMapKey
import spray.json._

/**
 * A key of a `JsonHMap`.
 * @constructor creates a new key and adds it to a registry.
 * @param sym the JSON key associated with this map key
 * @param reg the registry with which this key is to be associated
 * @param jsonFormat a `JsonFormat` which enables a key to serialize and
 *        deserialize its associated value
 * @tparam V the type of the value associated with this key
 */
abstract class JsonHMapKey[V](val sym: Symbol)(implicit reg: JsonKeyRegistry, jsonFormat: JsonFormat[V]) extends HMapKey[V] {
  reg.keys += (sym -> this)

  override def toKey: JsonHMapKey[Value] = this

  /**
   * Converts an object of this key's value type to JSON.
   * @param v the object to convert
   * @return the given object as a JSON value.
   */
  def toJson(v: Value): JsValue = jsonFormat.write(v)

  /**
   * Converts a JSON value to an object of this key's value type.
   * @param v the JSON value to convert
   * @return the given JSON value as an object.
   */
  def toValue(v: JsValue): Value = jsonFormat.read(v)

  override def toString: String = sym.toString()
}
