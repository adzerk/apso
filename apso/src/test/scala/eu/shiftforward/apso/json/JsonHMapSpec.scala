package eu.shiftforward.apso.json

import org.specs2.mutable.Specification
import eu.shiftforward.apso.collection.{ HMap, HMapKey }

import JsonHMap._
import spray.json._
import spray.json.DefaultJsonProtocol._

class JsonHMapSpec extends Specification {

  "A JsonHMap" should {

    implicit val reg = new JsonKeyRegistry {}

    val Key1 = new JsonHMapKey[Int]('key1) {}
    val Key2 = new JsonHMapKey[String]('key2) {}
    val Key3 = new JsonHMapKey[List[Boolean]]('key3) {}
    val Key4 = new JsonHMapKey[Int]('key4) {}

    "be correctly created from a list of key value pairs" in {
      val map = JsonHMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      map.entries.toSet mustEqual Set(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))
    }

    "be correctly created from JSON" in {
      val json =
        """
          |{
          |  "key1": 4,
          |  "key2": "s",
          |  "key3": [ false, true ]
          |}
        """.stripMargin

      val map = json.asJson.convertTo[JsonHMap]
      map.entries.toSet mustEqual Set(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      val invalidJson =
        """
          |{
          |  "key1": 4,
          |  "key2": 346,
          |  "key3": [ false, true ]
          |}
        """.stripMargin

      invalidJson.asJson.convertTo[JsonHMap] must throwA[Exception]
    }

    "be correctly created from JSON with unspecified key types" in {
      implicit val reg = new JsonKeyRegistry {}

      val json =
        """
          |{
          |  "key1": 4,
          |  "key2": "s",
          |  "key3": [ false, true ],
          |  "key4": {
          |    "key5": true,
          |    "key6": "hello"
          |  }
          |}
        """.stripMargin

      val map = json.asJson.convertTo[JsonHMap]
      map(reg.keys('key1)) mustEqual JsNumber(4)
      map(reg.keys('key2)) mustEqual JsString("s")
      map(reg.keys('key3)) mustEqual JsArray(JsBoolean(false), JsBoolean(true))
      map(reg.keys('key4)) mustEqual JsObject("key5" -> JsBoolean(true), "key6" -> JsString("hello"))
    }

    "be correctly converted to JSON" in {
      val map = JsonHMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      val json =
        """
          |{
          |  "key1": 4,
          |  "key2": "s",
          |  "key3": [ false, true ]
          |}
        """.stripMargin

      map.toJson.compactPrint mustEqual json.asJson.compactPrint
    }
  }
}
