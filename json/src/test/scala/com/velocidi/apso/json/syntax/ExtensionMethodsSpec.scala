package com.velocidi.apso.json.syntax

import scala.util.{Failure, Success}

import io.circe.CursorOp.DownField
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Json}
import org.specs2.mutable.Specification

class ExtensionMethodsSpec extends Specification {

  "emapPrettyTry" should {
    case class TestClass(str: String)
    implicit val decoder: Decoder[TestClass] = Decoder[String].emapPrettyTry { s =>
      if (s == "error") Failure(new Exception("foo"))
      else Success(TestClass(s))
    }

    "return the correct value if f returns a successful Try" in {
      decoder.decodeJson(Json.fromString("good")) must beRight(TestClass("good"))
    }

    "return a DecodingFailure without a stack trace if f returns a failure Try" in {
      decoder.decodeJson(Json.fromString("error")) must beLeft(DecodingFailure("foo", List.empty))
    }

    "return a DecodingFailure with corrects ops if f returns a failure Try in a nested json decoding" in {
      val result: Result[TestClass] =
        Json.obj("1" -> Json.obj("2" := "error")).hcursor.downField("1").downField("2").as[TestClass]
      result must beLeft(DecodingFailure("foo", List(DownField("2"), DownField("1"))))
    }
  }
}
