package com.kevel.apso.circe

import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.syntax._
import org.specs2.mutable.Specification

import com.kevel.apso.circe.Implicits._

class ImplicitsSpec extends Specification {
  "The Apso Json Implicits should" should {

    "provide a method to create a JSON object from complete paths" in {
      val expectedJson = json"""{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      "create a JSON object from complete paths" in {
        val res =
          fromFullPaths(List("a.b.c" -> 1.asJson, "a.b.d.e" -> 3.asJson, "a.f" -> 5.asJson, "g" -> 4.asJson))

        res mustEqual expectedJson
      }

      "create a JSON object from complete paths (with a custom separator)" in {
        val res =
          fromFullPaths(List("a-b-c" -> 1.asJson, "a-b-d-e" -> 3.asJson, "a-f" -> 5.asJson, "g" -> 4.asJson), "-")

        res mustEqual expectedJson
      }

      "not throw a StackOverflowError for large lists of paths" in {
        val paths = (1 to 10000).map(i => (s"a.v$i", i.asJson)).toList
        fromFullPaths(paths, ".") must not(throwAn[StackOverflowError])
      }

      "giving precedence to the last path value if duplicate paths exist" in {
        val json1 = fromFullPaths(List("a" -> 1.asJson, "a" -> 2.asJson, "a" -> 3.asJson))
        json1 mustEqual json"""{"a": 3}"""

        val json2 = fromFullPaths(List("a.b.c" -> 1.asJson, "a.b" -> 2.asJson, "a" -> 3.asJson))
        json2 mustEqual json"""{"a": 3}"""
      }
    }

    "provide a method to get the key set of a JSON Object" in {
      val obj = json"""{"a":1,"b":{"c":2},"d":null}"""
      obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
      obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
      obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")

      1.asJson.flattenedKeySet() === Set.empty
      json"""{}""".flattenedKeySet() === Set.empty
      json"""[{"a":1}]""".flattenedKeySet() === Set.empty
    }

    "provide a method to get the key-value set of a JSON Object" in {
      val obj = json"""{"a":1,"b":{"c":2},"d":null}"""
      obj.flattenedKeyValueSet(".") === Set(("a", Json.fromInt(1)), ("b.c", Json.fromInt(2)), ("d", Json.Null))
      obj.flattenedKeyValueSet("/") === Set(("a", Json.fromInt(1)), ("b/c", Json.fromInt(2)), ("d", Json.Null))

      1.asJson.flattenedKeyValueSet() === Set.empty
      json"""{}""".flattenedKeyValueSet() === Set.empty
      json"""[{"a":1}]""".flattenedKeyValueSet() === Set.empty
    }

    "provide a method to get a field from a JSON object" in {
      val obj = json"""{"a":"abc","b":{"c":2},"d":null}"""

      obj.getField[Int]("b.c") must beSome(2)
      obj.getField[Int]("b,c", ',') must beSome(2)
      obj.getField[String]("a") must beSome("abc")
    }

    "provide a method to delete a field from a JSON object" in {
      val obj = json"""{"a":"abc","b":{"c":2},"d":null}"""

      obj.deleteField("b.c") must beEqualTo(json"""{"a":"abc","b":{},"d":null}""")
      obj.deleteField("a") must beEqualTo(json"""{"b":{"c":2},"d":null}""")
      obj.deleteField("e") must beEqualTo(obj)
      obj.deleteField("b.c.d") must beEqualTo(obj)
      obj.deleteField("") must throwAn[IllegalArgumentException]
    }

    "provide extension methods for encoders" in {
      case class Foo(a: Int, b: Option[String])
      "which removes null values" in {
        val encoderWithouNulls = deriveEncoder[Foo].withoutNulls

        Foo(1, None).asJson(encoderWithouNulls) mustEqual json"""{ "a": 1 }"""
      }

      "which add fields" in {
        val encoderWithExtraField = deriveEncoder[Foo].withExtraField("c", 2.asJson)

        Foo(1, Some("x")).asJson(encoderWithExtraField) mustEqual json"""{ "a": 1, "b": "x", "c": 2 }"""
      }
    }
  }
}
