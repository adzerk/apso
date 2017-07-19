package eu.shiftforward.apso.json

import eu.shiftforward.apso.collection.HMap
import spray.json.{ JsValue, RootJsonFormat }
import scala.collection.mutable.{ Map => MutableMap, ListBuffer }
import spray.json.DefaultJsonProtocol._

/**
 * A class containing a mapping from JSON keys to keys of a `JsonHMap`. A
 * `JsonKeyRegistry` must be present both when a key is created and when a
 * conversion is to be done between a map and JSON.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
trait JsonKeyRegistry {

  /**
   * The map that maps JSON keys to keys of a `JsonHMap`.
   */
  val keys = MutableMap[Symbol, JsonHMapKey[_]]()
}

/**
 * Object that defines an `HMap` with JSON (de)serialization capabilities.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
object JsonHMap {

  /**
   * The type of an `HMap` with JSON (de)serialization capabilities.
   */
  type JsonHMap = HMap[JsonHMapKey]

  /**
   * Creates a `JsonHMap` with the given entries.
   * @param entries the entries to be present in the new map
   * @return a new map containing the given entries.
   */
  def apply(entries: (JsonHMapKey[V], V) forSome { type V }*): JsonHMap =
    new HMap(ListBuffer(entries: _*))

  /**
   * Implicit method for creating `JsonFormats` that handle serialization of
   * `JsonHMaps`.
   * @param reg the registry used to translate between JSON keys and map keys
   * @return a `JsonFormat` for `JsonHMaps` that uses the specified registry.
   */
  implicit def caseJsonFormat(implicit reg: JsonKeyRegistry) = new RootJsonFormat[JsonHMap] {

    def read(json: JsValue): JsonHMap = {
      val caseObj = JsonHMap()
      json.asJsObject.fields.foreach {
        case (k, v) =>
          reg.keys.get(Symbol(k)) match {
            case Some(caseKey) =>
              caseObj.put(caseKey.toKey, caseKey.toValue(v))
            case None =>
              caseObj.put(new JsonHMapKey[JsValue](Symbol(k)) {}, v)
          }
      }
      caseObj
    }

    def write(map: JsonHMap): JsValue = {
      Map(map.map { entry =>
        entry._1.sym.name -> entry._1.toJson(entry._2.asInstanceOf[entry._1.Value])
      }: _*).toJson
    }
  }
}
