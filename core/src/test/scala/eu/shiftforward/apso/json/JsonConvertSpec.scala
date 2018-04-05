package eu.shiftforward.apso.json

import scala.collection.JavaConverters._

import io.circe.Json
import io.circe.parser.decode
import org.specs2.matcher.EitherMatchers
import org.specs2.mutable.Specification

class JsonConvertSpec extends Specification with EitherMatchers {
  "JsonConvert" should {
    "be able to convert scala objects to JSON" in {
      JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual decode[Json]("""{ "a": 2, "b": { "3": 7 }}""").right.get
      JsonConvert.toJson(2) mustEqual decode[Json]("2").right.get
      JsonConvert.toJson("2") mustEqual decode[Json](""""2"""").right.get
      JsonConvert.toJson(2.5) mustEqual decode[Json]("2.5").right.get
      JsonConvert.toJson(List(1, 2, 3, 4)) mustEqual decode[Json]("""[1, 2, 3, 4]""").right.get
    }

    "be able to convert java objects to JSON" in {
      JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual decode[Json]("""{ "a": 2, "b": { "3": 7 }}""").right.get
      JsonConvert.toJson(List(1, 2, 3, 4).asJava) mustEqual decode[Json]("""[1, 2, 3, 4]""").right.get
    }
  }
}
