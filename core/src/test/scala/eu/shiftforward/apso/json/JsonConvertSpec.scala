package eu.shiftforward.apso.json

import scala.collection.JavaConverters._

import io.circe.Json
import io.circe.parser.decode
import org.specs2.matcher.EitherMatchers
import org.specs2.mutable.Specification
import spray.json._

class JsonConvertSpec extends Specification with EitherMatchers {
  "JsonConvert" should {
    "be able to produce Spray Json models" in {
      "by converting scala objects to JSON" in {
        JsonConvert.toCirceJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual decode[Json]("""{ "a": 2, "b": { "3": 7 }}""").right.get
        JsonConvert.toCirceJson(2) mustEqual decode[Json]("2").right.get
        JsonConvert.toCirceJson("2") mustEqual decode[Json](""""2"""").right.get
        JsonConvert.toCirceJson(2.5) mustEqual decode[Json]("2.5").right.get
        JsonConvert.toCirceJson(List(1, 2, 3, 4)) mustEqual decode[Json]("""[1, 2, 3, 4]""").right.get
      }

      "by converting java objects to JSON" in {
        JsonConvert.toCirceJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual decode[Json]("""{ "a": 2, "b": { "3": 7 }}""").right.get
        JsonConvert.toCirceJson(List(1, 2, 3, 4).asJava) mustEqual decode[Json]("""[1, 2, 3, 4]""").right.get
      }
    }

    "be able to produce Circe Json models" in {
      "by converting scala objects to JSON" in {
        JsonConvert.toSprayJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual """{ "a": 2, "b": { "3": 7 }}""".parseJson
        JsonConvert.toSprayJson(2) mustEqual "2".parseJson
        JsonConvert.toSprayJson("2") mustEqual """"2"""".parseJson
        JsonConvert.toSprayJson(2.5) mustEqual "2.5".parseJson
        JsonConvert.toSprayJson(List(1, 2, 3, 4)) mustEqual """[1, 2, 3, 4]""".parseJson
      }

      "by converting java objects to JSON" in {
        JsonConvert.toSprayJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual """{ "a": 2, "b": { "3": 7 }}""".parseJson
        JsonConvert.toSprayJson(List(1, 2, 3, 4).asJava) mustEqual """[1, 2, 3, 4]""".parseJson
      }
    }
  }
}
