package com.velocidi.apso.json

import scala.util.Try

import org.specs2.mutable.Specification
import shapeless._
import shapeless.Nat._
import spray.json.DefaultJsonProtocol._
import spray.json._

@deprecated("This will be removed in a future version.", "2019/10/23")
class JsonFormatBuilderSpec extends Specification {

  case class Test(a: Int, b: List[String], c: Double)
  case class Test2(a: Int, b2: List[Boolean], c: Double)
  case class Test3(b: List[String], c: Double)
  case class Test4(a: Option[Int], b: Option[String])

  "A JsonFormatBuilder" should {

    "allow constructing JSON formats by adding fields incrementally" in {
      val builder = JsonFormatBuilder()
        .field[Int]("a")
        .field[List[String]]("b")

      val jf1 = builder.jsonFormat[Test](
        { case a :: b :: HNil => Test(a, b, -1.0) },
        { test => test.a :: test.b :: HNil })

      """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf1) mustEqual Test(3, List("x", "y"), -1.0)
      """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf1) mustEqual Test(3, List("x", "y"), -1.0)

      Test(3, List("x", "y"), -1.0).toJson(jf1) mustEqual """{ "a": 3, "b": ["x", "y"] }""".parseJson

      val builder2 = builder.field[Double]("c", 0.0)

      val jf2 = builder2.jsonFormat[Test](
        { case a :: b :: c :: HNil => Test(a, b, c) },
        { test => test.a :: test.b :: test.c :: HNil })

      """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf2) mustEqual Test(3, List("x", "y"), 0.0)
      """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf2) mustEqual Test(3, List("x", "y"), 3.0)

      Test(3, List("x", "y"), 0.0).toJson(jf2) mustEqual """{ "a": 3, "b": ["x", "y"], "c": 0.0 }""".parseJson
    }

    "allow replacing fields already added by other definitions" in {
      val builder = JsonFormatBuilder()
        .field[Int]("a")
        .field[List[String]]("b")
        .field[Double]("c", 0.0)

      val builder2 = builder.replaceField[_1, List[Boolean]]("b2", List(true))

      val jf2 = builder2.jsonFormat[Test2](
        { case a :: b :: c :: HNil => Test2(a, b, c) },
        { test => test.a :: test.b2 :: test.c :: HNil })

      """{ "a": 3 }""".parseJson.convertTo[Test2](jf2) mustEqual Test2(3, List(true), 0.0)
      """{ "a": 3, "b2": ["x", "y"] }""".parseJson.convertTo[Test2](jf2) must throwA[DeserializationException]
      """{ "a": 3, "b2": [true, false], "c": 3.0 }""".parseJson.convertTo[Test2](jf2) mustEqual Test2(3, List(true, false), 3.0)

      Test2(3, List(true, false), 2.5).toJson(jf2) mustEqual """{ "a": 3, "b2": [true, false], "c": 2.5 }""".parseJson
    }

    "allow removing fields already added" in {
      val builder = JsonFormatBuilder()
        .field[Int]("a")
        .field[List[String]]("b")
        .field[Double]("c", 0.0)

      val builder2 = builder.removeField[_0]

      val jf2 = builder2.jsonFormat[Test3](
        { case b :: c :: HNil => Test3(b, c) },
        { test => test.b :: test.c :: HNil })

      """{ "b": ["x", "y"] }""".parseJson.convertTo[Test3](jf2) mustEqual Test3(List("x", "y"), 0.0)
      """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test3](jf2) mustEqual Test3(List("x", "y"), 3.0)

      Test3(List("x", "y"), 2.5).toJson(jf2) mustEqual """{ "b": ["x", "y"], "c": 2.5 }""".parseJson
    }

    "allow defining optional fields" in {
      val builder = JsonFormatBuilder()
        .optionalField[Int]("a")
        .optionalField[String]("b")

      val jf1 = builder.jsonFormat[Test4](
        { case a :: b :: HNil => Test4(a, b) },
        { foo => foo.a :: foo.b :: HNil })

      """{ "a": 3, "b": "hello" }""".parseJson.convertTo[Test4](jf1) mustEqual Test4(Some(3), Some("hello"))
      """{ "a": 3 }""".parseJson.convertTo[Test4](jf1) mustEqual Test4(Some(3), None)
      """{ "a": 3, "b": null }""".parseJson.convertTo[Test4](jf1) mustEqual Test4(Some(3), None)
      """{ }""".parseJson.convertTo[Test4](jf1) mustEqual Test4(None, None)

      Test4(Some(3), Some("x")).toJson(jf1) mustEqual """{ "a": 3, "b": "x" }""".parseJson
      Test4(Some(3), None).toJson(jf1) mustEqual """{ "a": 3 }""".parseJson
      Test4(None, None).toJson(jf1) mustEqual """{ }""".parseJson
    }

    "allow using single JSON Readers and JSON Writers" in {
      val builder = JsonFormatBuilder()
        .optionalField[Int]("a")
        .optionalField[String]("b")

      val jr = builder.jsonReader[Test4]({ case a :: b :: HNil => Test4(a, b) })

      """{ "a": 3, "b": "hello" }""".parseJson.convertTo[Test4](jr) mustEqual Test4(Some(3), Some("hello"))
      """{ "a": 3 }""".parseJson.convertTo[Test4](jr) mustEqual Test4(Some(3), None)
      """{ "a": 3, "b": null }""".parseJson.convertTo[Test4](jr) mustEqual Test4(Some(3), None)
      """{ }""".parseJson.convertTo[Test4](jr) mustEqual Test4(None, None)

      val jw = builder.jsonWriter[Test4]({ foo => foo.a :: foo.b :: HNil })

      Test4(Some(3), Some("x")).toJson(jw) mustEqual """{ "a": 3, "b": "x" }""".parseJson
      Test4(Some(3), None).toJson(jw) mustEqual """{ "a": 3 }""".parseJson
      Test4(None, None).toJson(jw) mustEqual """{ }""".parseJson
    }

    "allow defining custom JSON Formats with transformations and custom error handling" in {
      val builder = JsonFormatBuilder()
        .field[Int]("a")
        .field[List[String]]("b")
        .field[Double]("c", 0.0)

      val jf = builder.customJsonFormat[Test](
        { obj: JsObject => JsObject((obj.fields + ("a" -> 0.toJson)).toList: _*) },
        { case a :: b :: c :: HNil => Test(a, b, c) },
        { test: Test => test.a :: test.b.tail :: test.c :: HNil },
        { (_: Test, obj: JsObject) => JsObject((obj.fields + ("c" -> 1.5.toJson)).toList: _*) },
        (json: JsValue, _: Throwable) =>
          json match {
            case JsString(str) => Test(0, List(str), 0.0)
            case _ => throw new Exception("Caught error!")
          })

      """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf) mustEqual Test(0, List("x", "y"), 3.0)
      Test(0, List("x", "y"), 3.0).toJson(jf) mustEqual """{ "a": 0, "b": ["y"], "c": 1.5 }""".parseJson
      Try("""{ "a": "asd" }""".parseJson.convertTo[Test](jf)) must beFailedTry.withThrowable[Exception]("Caught error!")
      """"string"""".parseJson.convertTo[Test](jf) mustEqual Test(0, List("string"), 0.0)
    }

    "interpret null as empty values when a default value is supplied" in {
      case class Foo(a: Int)

      val builder = JsonFormatBuilder()
        .field[Int]("a", 2)

      val jr = builder.jsonReader({ case a :: HNil => Foo(a) })

      """{ "a": null }""".parseJson.convertTo[Foo](jr) mustEqual Foo(2)
    }

    "throw a DeserializationException when a required field is missing" in {
      case class Foo(a: Int)

      val builder = JsonFormatBuilder()
        .field[Int]("a")

      val jr = builder.jsonReader({ case a :: HNil => Foo(a) })

      """{ "a": null }""".parseJson.convertTo[Foo](jr) must throwA[DeserializationException]
      """{}""".parseJson.convertTo[Foo](jr) must throwA[DeserializationException]
    }
  }
}
