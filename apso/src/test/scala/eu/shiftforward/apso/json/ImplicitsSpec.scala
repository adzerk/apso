package eu.shiftforward.apso.json

import org.specs2.mutable._
import spray.json.DefaultJsonProtocol._
import spray.json._

import eu.shiftforward.apso.json.Implicits._

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

    "provide a method to get the key set of a JsObject" in {
      val obj = """{"a":1,"b":{"c":2},"d":null}""".parseJson.asJsObject
      obj.flattenedKeySet(".", ignoreNull = true) === Set("a", "b.c")
      obj.flattenedKeySet(".", ignoreNull = false) === Set("a", "b.c", "d")
      obj.flattenedKeySet("/", ignoreNull = true) === Set("a", "b/c")
    }
  }
}
