package eu.shiftforward.apso.json

import org.specs2.mutable.Specification
import eu.shiftforward.apso.collection.{HMap, HMapKey}

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
