package com.velocidi.apso.json

import scala.collection.JavaConverters._

import io.circe.parser._
import org.specs2.matcher.EitherMatchers
import org.specs2.mutable.Specification

class JsonConvertSpec extends Specification with EitherMatchers {
  "JsonConvert" should {
    "be able to produce Spray Json models" in {
      "by converting scala objects to JSON" in {
        JsonConvert.toCirceJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual parse("""{ "a": 2, "b": { "3": 7 }}""").right.get
        JsonConvert.toCirceJson(2) mustEqual parse("2").right.get
        JsonConvert.toCirceJson("2") mustEqual parse(""""2"""").right.get
        JsonConvert.toCirceJson(2.5) mustEqual parse("2.5").right.get
        JsonConvert.toCirceJson(List(1, 2, 3, 4)) mustEqual parse("""[1, 2, 3, 4]""").right.get
      }

      "by converting java objects to JSON" in {
        JsonConvert.toCirceJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual parse("""{ "a": 2, "b": { "3": 7 }}""").right.get
        JsonConvert.toCirceJson(List(1, 2, 3, 4).asJava) mustEqual parse("""[1, 2, 3, 4]""").right.get
        JsonConvert.toCirceJson(Array(1, 2, 3, 4)) mustEqual parse("""[1, 2, 3, 4]""").right.get
      }
    }
  }
}
