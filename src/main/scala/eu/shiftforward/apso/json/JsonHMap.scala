package eu.shiftforward.apso.json

import eu.shiftforward.apso.collection.HMap
import spray.json.{JsValue, RootJsonFormat}
import scala.collection.mutable.{Map => MutableMap, ListBuffer}
import spray.json.DefaultJsonProtocol._

trait JsonKeyRegistry {
  val keys = MutableMap[Symbol, JsonHMapKey[_]]()
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
