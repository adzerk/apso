package eu.shiftforward.apso.json

import org.specs2.mutable._
import spray.json._
import eu.shiftforward.apso.json.Implicits._

class ImplicitsSpec extends Specification {
  "The Apso Json Implicits should" should {
    "provide a method to access json fields from a complete path" in {
      val source =
        """{ "a": {"b": {"c": 1, "d": {"e": 3}}, "f": 5}, "g": 4 }"""

      val jsonAst = source.parseJson

      jsonAst.getPath[JsNumber]("a.b.c") must beSome(JsNumber(1))
      jsonAst.getPath[JsNumber]("g") must beSome(JsNumber(4))
      jsonAst.getPath[JsNumber]("a.b.d.e") must beSome(JsNumber(3))
      jsonAst.getPath[JsNumber]("a.f") must beSome(JsNumber(5))
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
  }
}
