package com.velocidi.apso.json

import io.circe.Json
import org.specs2.mutable._
import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.syntax._
import io.circe.parser._
import spray.json.DefaultJsonProtocol._
import spray.json._

import com.velocidi.apso.json.Implicits._

class ImplicitsSpec extends Specification {
  "The Apso Json Implicits should" should {

    "convert JsonValues to values" in {
      "asd".toJson.toValue === "asd"
      123.toJson.toValue === 123
      Map("x" -> Map("y" -> 1)).toJson.toValue === Map("x" -> Map("y" -> 1))
      JsArray(Vector(1.toJson, 2.toJson, "a".toJson)).toValue === List(1, 2, "a")
      true.toJson.toValue === true
    }

    "provide a merge method for JSON arrays" in {
      val source1 =
        """[1, 2, 3]"""

      val source2 =
        """["a", "b", "c"]"""

      val res =
        """[1, 2, 3, "a", "b", "c"]"""

      source1.parseJson.merge(source2.parseJson) mustEqual res.parseJson
    }

    "provide a merge method for JSON objects" in {
      val source1 =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      val source2 =
        """{ "a": {"b": {"h": 7, "i": 6}}, "j": 8 }"""

      val res =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}, "h": 7, "i": 6}, "f": 5}, "g": 4, "j": 8 }"""

      source1.parseJson.merge(source2.parseJson) mustEqual res.parseJson
    }

    "provide a merge method for JSON objects, with conflicts" in {
      val source1 =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      val source2 =
        """{ "a": {"b": {"c": 9, "h": 7, "i": 6}}, "j": 8, "g": [{"key": "val"}] }"""

      val res =
        """{ "a": {"b": {"c": 9, "d": {"e": 3}, "h": 7, "i": 6}, "f": 5}, "g": [{"key": "val"}], "j": 8 }"""

      source1.parseJson.merge(source2.parseJson, failOnConflict = false) mustEqual res.parseJson
    }

    "provide a method to create a JSON object from complete paths" in {
      val expected = """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      "create a spray-json JSON object from complete paths" in {
        val res = fromFullPaths(
          List(
            "a.b.c" -> JsNumber(1),
            "a.b.d.e" -> JsNumber(3),
            "a.f" -> JsNumber(5),
            "g" -> JsNumber(4)))

        res mustEqual expected.parseJson
      }

      "create a circe JSON object from complete paths" in {
        val res = fromCirceFullPaths(
          List(
            "a.b.c" -> 1.asJson,
            "a.b.d.e" -> 3.asJson,
            "a.f" -> 5.asJson,
            "g" -> 4.asJson))

        res mustEqual parse(expected).fold(throw _, identity)
      }

      "create a spray-json JSON object from complete paths (with a custom separator)" in {
        val res = fromFullPaths(
          List(
            "a-b-c" -> JsNumber(1),
            "a-b-d-e" -> JsNumber(3),
            "a-f" -> JsNumber(5),
            "g" -> JsNumber(4)), "-")

        res mustEqual expected.parseJson
      }

      "create a circe JSON object from complete paths (with a custom separator)" in {
        val res = fromCirceFullPaths(
          List(
            "a-b-c" -> 1.asJson,
            "a-b-d-e" -> 3.asJson,
            "a-f" -> 5.asJson,
            "g" -> 4.asJson), "-")

        res mustEqual parse(expected).fold(throw _, identity)
      }
    }

    "provide a method to get the flattened key-value set of a JSON Object" in {
      val jsonString = """{"a":1,"b":{"c":2}, "b.c": 3, "d":null}"""

      "for spray-json JSON objects" in {
        val obj = jsonString.parseJson.asJsObject

        obj.flattenedKeyValueSet(".") === Set("a" -> JsNumber(1), "b.c" -> JsNumber(2), "b.c" -> JsNumber(3), "d" -> JsNull)
        obj.flattenedKeyValueSet("/") === Set("a" -> JsNumber(1), "b/c" -> JsNumber(2), "b.c" -> JsNumber(3), "d" -> JsNull)
      }

      "for circe JSON objects" in {
        val obj = parse(jsonString).fold(throw _, identity)

        obj.flattenedKeyValueSet(".") === Set("a" -> Json.fromInt(1), "b.c" -> Json.fromInt(2), "b.c" -> Json.fromInt(3), "d" -> Json.Null)
        obj.flattenedKeyValueSet("/") === Set("a" -> Json.fromInt(1), "b/c" -> Json.fromInt(2), "b.c" -> Json.fromInt(3), "d" -> Json.Null)
        1.asJson.flattenedKeyValueSet() === Set.empty
      }
    }

    "provide a method to get the key set of a JSON Object" in {
      val jsonString = """{"a":1,"b":{"c":2},"d":null}"""

      "for spray-json JSON objects" in {
        val obj = jsonString.parseJson.asJsObject

        obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
        obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
        obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")
      }

      "for circe JSON objects" in {
        val obj = parse(jsonString).fold(throw _, identity)

        obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
        obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
        obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")
        1.asJson.flattenedKeySet() === Set.empty
      }
    }

    "provide a method to get a field from a JSON object" in {
      val obj = parse("""{"a":"abc","b":{"c":2},"d":null}""").fold(throw _, identity)

      obj.getField[Int]("b.c") must beSome(2)
      obj.getField[Int]("b,c", ',') must beSome(2)
      obj.getField[String]("a") must beSome("abc")
    }

    "provide a method to delete a field from a JSON object" in {
      val obj = json"""{"a":"abc","b":{"c":2},"d":null}"""

      obj.deleteField("b.c") must beEqualTo(json"""{"a":"abc","b":{},"d":null}""")
      obj.deleteField("a") must beEqualTo(json"""{"b":{"c":2},"d":null}""")
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
