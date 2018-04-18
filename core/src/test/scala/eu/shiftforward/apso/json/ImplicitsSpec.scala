package eu.shiftforward.apso.json

import org.specs2.mutable._
import io.circe.syntax._
import io.circe.parser._
import spray.json.DefaultJsonProtocol._
import spray.json._

import eu.shiftforward.apso.json.Implicits.{ ApsoJsonObject => _, _ }

class ImplicitsSpec extends Specification {
  "The Apso Json Implicits should" should {

    "convert JsonValues to values" in {
      "asd".toJson.toValue === "asd"
      123.toJson.toValue === 123
      Map("x" -> Map("y" -> 1)).toJson.toValue === Map("x" -> Map("y" -> 1))
      JsArray(Vector(1.toJson, 2.toJson, "a".toJson)).toValue === List(1, 2, "a")
      true.toJson.toValue === true
    }

    "provide a merge method for json arrays" in {
      val source1 =
        """[1, 2, 3]"""

      val source2 =
        """["a", "b", "c"]"""

      val res =
        """[1, 2, 3, "a", "b", "c"]"""

      source1.parseJson.merge(source2.parseJson) mustEqual res.parseJson
    }

    "provide a merge method for json objects" in {
      val source1 =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      val source2 =
        """{ "a": {"b": {"h": 7, "i": 6}}, "j": 8 }"""

      val res =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}, "h": 7, "i": 6}, "f": 5}, "g": 4, "j": 8 }"""

      source1.parseJson.merge(source2.parseJson) mustEqual res.parseJson
    }

    "provide a merge method for json objects, with conflicts" in {
      val source1 =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      val source2 =
        """{ "a": {"b": {"c": 9, "h": 7, "i": 6}}, "j": 8, "g": [{"key": "val"}] }"""

      val res =
        """{ "a": {"b": {"c": 9, "d": {"e": 3}, "h": 7, "i": 6}, "f": 5}, "g": [{"key": "val"}], "j": 8 }"""

      source1.parseJson.merge(source2.parseJson, failOnConflict = false) mustEqual res.parseJson
    }

    "provide a method to create a json object from complete paths" in {
      val res = fromFullPaths(
        List(
          "a.b.c" -> JsNumber(1),
          "a.b.d.e" -> JsNumber(3),
          "a.f" -> JsNumber(5),
          "g" -> JsNumber(4)))

      val expected =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      res mustEqual expected.parseJson
    }

    "provide a method to create a json object from complete paths (with a custom separator)" in {
      val res = fromFullPaths(
        List(
          "a-b-c" -> JsNumber(1),
          "a-b-d-e" -> JsNumber(3),
          "a-f" -> JsNumber(5),
          "g" -> JsNumber(4)), "-")

      val expected =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      res mustEqual expected.parseJson
    }

    "provide a method to create a circe json object from complete paths" in {
      val res = fromCirceFullPaths(
        List(
          "a.b.c" -> 1.asJson,
          "a.b.d.e" -> 3.asJson,
          "a.f" -> 5.asJson,
          "g" -> 4.asJson))

      val expected =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      res mustEqual parse(expected).toTry.get
    }

    "provide a method to create a circe json object from complete paths (with a custom separator)" in {
      val res = fromCirceFullPaths(
        List(
          "a-b-c" -> 1.asJson,
          "a-b-d-e" -> 3.asJson,
          "a-f" -> 5.asJson,
          "g" -> 4.asJson), "-")

      val expected =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      res mustEqual parse(expected).right.get
    }

    "provide a method to get the key set of a JsObject" in {
      val obj = """{"a":1,"b":{"c":2},"d":null}""".parseJson.asJsObject
      obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
      obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
      obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")
    }

    "provide a method to get the key set of a JSON object" in {
      import eu.shiftforward.apso.json.Implicits.{ ApsoJsonObject }

      val obj = parse("""{"a":1,"b":{"c":2},"d":null}""").right.get
      obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
      obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
      obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")
      1.asJson.flattenedKeySet() === Set.empty
    }

    "provide a method to get a field from a JSON object" in {
      import eu.shiftforward.apso.json.Implicits.ApsoJsonObject

      val obj = parse("""{"a":"abc","b":{"c":2},"d":null}""").right.get
      obj.getField[Int]("b.c") must beSome(2)
      obj.getField[Int]("b,c", ',') must beSome(2)
      obj.getField[String]("a") must beSome("abc")
    }

    "provide a method to delete a field from a JSON object" in {
      import eu.shiftforward.apso.json.Implicits.ApsoJsonObject

      val obj = parse("""{"a":"abc","b":{"c":2},"d":null}""").right.get
      obj.deleteField("b.c") must beSome(parse("""{"a":"abc","b":{},"d":null}""").right.get)
      obj.deleteField("a") must beSome(parse("""{"b":{"c":2},"d":null}""").right.get)
    }
  }
}
