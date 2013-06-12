package eu.shiftforward.apso.json

import eu.shiftforward.apso.collection.{HMap, HMapKey}
import scala.collection.mutable.{Map => MutableMap, ListBuffer}
import spray.json._
import spray.json.DefaultJsonProtocol._

trait JsonKeyRegistry {
  val keys = MutableMap[Symbol, JsonHMapKey[_]]()
}

abstract class JsonHMapKey[V](val sym: Symbol)(implicit val reg: JsonKeyRegistry, val jsonFormat: JsonFormat[V]) extends HMapKey[V] {
  reg.keys += (sym -> this)

  override def toKey: JsonHMapKey[Value] = this
  def toJson(v: Value): JsValue = jsonFormat.write(v)
  def toValue(v: JsValue): Value = jsonFormat.read(v)
}

object JsonHMap {
  type JsonHMap = HMap[JsonHMapKey]

  def apply(entries: (JsonHMapKey[V], V) forSome { type V }*): JsonHMap =
    new HMap(ListBuffer(entries: _*))

  implicit def caseJsonFormat(implicit reg: JsonKeyRegistry) = new RootJsonFormat[JsonHMap] {
    def read(json: JsValue): JsonHMap = {
      val caseObj = JsonHMap()
      json.asJsObject.fields.foreach { case (k, v) =>
        reg.keys.get(Symbol(k)).map { caseKey =>
          caseObj.put(caseKey.toKey, caseKey.toValue(v))
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
