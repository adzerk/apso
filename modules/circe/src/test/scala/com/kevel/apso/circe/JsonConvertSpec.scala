package com.kevel.apso.circe

import scala.jdk.CollectionConverters._

import io.circe.parser._
import org.specs2.matcher.EitherMatchers
import org.specs2.mutable.Specification

class JsonConvertSpec extends Specification with EitherMatchers {
  "JsonConvert" should {
    "be able to produce Json models" in {
      "by converting scala objects to JSON" in {
        JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7))) mustEqual parse(
          """{ "a": 2, "b": { "3": 7 }}"""
        ).toTry.get
        JsonConvert.toJson(2) mustEqual parse("2").toTry.get
        JsonConvert.toJson("2") mustEqual parse(""""2"""").toTry.get
        JsonConvert.toJson(2.5) mustEqual parse("2.5").toTry.get
        JsonConvert.toJson(List(1, 2, 3, 4)) mustEqual parse("""[1, 2, 3, 4]""").toTry.get
      }

      "by converting java objects to JSON" in {
        JsonConvert.toJson(Map("a" -> 2, "b" -> Map(3 -> 7)).asJava) mustEqual parse(
          """{ "a": 2, "b": { "3": 7 }}"""
        ).toTry.get
        JsonConvert.toJson(List(1, 2, 3, 4).asJava) mustEqual parse("""[1, 2, 3, 4]""").toTry.get
        JsonConvert.toJson(Array(1, 2, 3, 4)) mustEqual parse("""[1, 2, 3, 4]""").toTry.get
      }
    }
  }
}
