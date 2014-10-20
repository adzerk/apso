package eu.shiftforward.apso.json

import spray.json._
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

class JsonConvertSpec extends Specification {
  "JsonConvert" should {
    "be able to convert scala objects to JSON" in {
      JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual """{ "a": 2, "b": { "3": 7 }}""".parseJson
      JsonConvert.toJson(2) mustEqual "2".parseJson
      JsonConvert.toJson("2") mustEqual """"2"""".parseJson
      JsonConvert.toJson(2.5) mustEqual "2.5".parseJson
      JsonConvert.toJson(List(1, 2, 3, 4)) mustEqual """[1, 2, 3, 4]""".parseJson
    }

    "be able to convert java objects to JSON" in {
      JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual """{ "a": 2, "b": { "3": 7 }}""".parseJson
      JsonConvert.toJson(List(1, 2, 3, 4).asJava) mustEqual """[1, 2, 3, 4]""".parseJson
    }
  }
}
