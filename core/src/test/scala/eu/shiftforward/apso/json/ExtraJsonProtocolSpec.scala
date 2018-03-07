package eu.shiftforward.apso.json

import java.net.URI

import scala.concurrent.duration._

import com.typesafe.config.{ Config, ConfigFactory }
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.joda.time.{ DateTime, Interval, LocalDate, Period }
import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol._
import spray.json._

class ExtraJsonProtocolSpec extends Specification {

  "The object ExtraJsonProtocol" should {
    import ExtraJsonProtocol._

    "provide a JsonFormat for FiniteDuration" in {
      val duration = 10.seconds
      val durationJsonString = """{"milliseconds":10000}"""

      duration.toJson.compactPrint mustEqual durationJsonString
      durationJsonString.parseJson.convertTo[FiniteDuration] mustEqual duration

      """{"seconds": 2}""".parseJson.convertTo[FiniteDuration] mustEqual 2.seconds
      """{"minutes": 2}""".parseJson.convertTo[FiniteDuration] mustEqual 2.minutes
      """{"hours":   2}""".parseJson.convertTo[FiniteDuration] mustEqual 2.hours
      """{"days":    2}""".parseJson.convertTo[FiniteDuration] mustEqual 2.days
      """{"meters":  2}""".parseJson.convertTo[FiniteDuration] must throwA[DeserializationException]

      """2""".parseJson.convertTo[FiniteDuration] mustEqual 2.milliseconds
      """"2s"""".parseJson.convertTo[FiniteDuration] mustEqual 2.seconds
      """"2m"""".parseJson.convertTo[FiniteDuration] mustEqual 2.minutes
      """"2h"""".parseJson.convertTo[FiniteDuration] mustEqual 2.hours
      """"2d"""".parseJson.convertTo[FiniteDuration] mustEqual 2.days
      """"garbagio"""".parseJson.convertTo[FiniteDuration] must throwA[DeserializationException]

      "true".parseJson.convertTo[FiniteDuration] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for FiniteDuration" in {
      val duration = 10.seconds
      val durationJsonString = """{"milliseconds":10000}"""

      duration.asJson.pretty(Printer.noSpaces) mustEqual durationJsonString
      parse(durationJsonString).right.flatMap(_.as[FiniteDuration]) must beRight(duration)

      def convert(v: String): Either[Error, FiniteDuration] =
        parse(v).right.flatMap(_.as[FiniteDuration])

      convert("""{"seconds": 2}""") must beRight(2.seconds)
      convert("""{"minutes": 2}""") must beRight(2.minutes)
      convert("""{"hours":   2}""") must beRight(2.hours)
      convert("""{"days":    2}""") must beRight(2.days)
      convert("""{"meters":  2}""") must beLeft

      convert("""2""") must beRight(2.milliseconds)
      convert(""""2s"""") must beRight(2.seconds)
      convert(""""2m"""") must beRight(2.minutes)
      convert(""""2h"""") must beRight(2.hours)
      convert(""""2d"""") must beRight(2.days)
      convert(""""garbagio"""") must beLeft

      convert("true") must beLeft
    }

    "provide a JsonFormat for Interval" in {
      val interval = new Interval(1000, 2000)
      val intervalJsonString = """{"startMillis":1000,"endMillis":2000}"""

      interval.toJson.compactPrint mustEqual intervalJsonString
      intervalJsonString.parseJson.convertTo[Interval] mustEqual interval
      """{"invalidObject":true}""".parseJson.convertTo[Interval] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for Interval" in {
      val interval = new Interval(1000, 2000)
      val intervalJsonString = """{"startMillis":1000,"endMillis":2000}"""

      interval.asJson.pretty(Printer.noSpaces) mustEqual intervalJsonString
      parse(intervalJsonString).right.flatMap(_.as[Interval]) must beRight(interval)
    }

    "provide a JsonFormat for Period" in {
      val pStrings = Seq("P1D", "P1M2D", "P1M2DT10H30M")
      pStrings.forall { s =>
        val period = new Period(s)
        period.toJson mustEqual JsString(s)
        s""""$s"""".parseJson.convertTo[Period] mustEqual period
      }

      """"garbage"""".parseJson.convertTo[Period] must throwA[DeserializationException]
      """"PXD"""".parseJson.convertTo[Period] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for Period" in {
      val pStrings = Seq("P1D", "P1M2D", "P1M2DT10H30M")
      pStrings.forall { s =>
        val period = new Period(s)
        period.asJson.pretty(Printer.noSpaces) mustEqual s""""$s""""
        parse(s""""$s"""").right.flatMap(_.as[Period]) must beRight(period)
      }

      parse(""""garbage"""").right.flatMap(_.as[Period]) must beLeft
      parse(""""PXD"""").right.flatMap(_.as[Period]) must beLeft
    }

    "provide a JsonFormat for URI" in {
      val uri = new URI("http://example.com")
      val uriJsonString = """"http://example.com""""

      uri.toJson.compactPrint mustEqual uriJsonString
      uriJsonString.parseJson.convertTo[URI] mustEqual uri
      "true".parseJson.convertTo[URI] must throwA[DeserializationException]
      """"{invalidUri}"""".parseJson.convertTo[URI] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for URI" in {
      val uri = new URI("http://example.com")
      val uriJsonString = """"http://example.com""""

      uri.asJson.pretty(Printer.noSpaces) mustEqual uriJsonString
      parse(uriJsonString).right.flatMap(_.as[URI]) must beRight(uri)
      parse("true").right.flatMap(_.as[URI]) must beLeft
      parse(""""{invalidUri}"""").right.flatMap(_.as[URI]) must beLeft
    }

    "provide a JsonFormat for Config" in {
      val config = ConfigFactory.parseString("""
        |a = 123
        |b {
        |  x = 1d
        |  y = "string"
        |}
      """.stripMargin)
      val configJsonString = """{"a":123,"b":{"x":"1d","y":"string"}}"""

      config.toJson.compactPrint mustEqual configJsonString
      configJsonString.parseJson.convertTo[Config] mustEqual config
      "true".parseJson.convertTo[Config] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for Config" in {
      val config = ConfigFactory.parseString("""
                                               |a = 123
                                               |b {
                                               |  x = 1d
                                               |  y = "string"
                                               |}
                                             """.stripMargin)
      val configJsonString = """{"a":123,"b":{"x":"1d","y":"string"}}"""

      config.asJson.pretty(Printer.noSpaces) mustEqual configJsonString
      parse(configJsonString).right.flatMap(_.as[Config]) must beRight(config)
      parse("true").right.flatMap(_.as[Config]) must beLeft
    }

    "provide a JsonFormat for DateTime" in {
      val dateTime = new DateTime("2016-01-01")
      val dateTimeJsonString = """"2016-01-01T00:00:00.000Z""""

      dateTime.toJson.compactPrint mustEqual dateTimeJsonString
      dateTimeJsonString.parseJson.convertTo[DateTime] mustEqual dateTime
      "true".parseJson.convertTo[DateTime] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for DateTime" in {
      val dateTime = new DateTime("2016-01-01")
      val dateTimeJsonString = """"2016-01-01T00:00:00.000Z""""

      dateTime.asJson.pretty(Printer.noSpaces) mustEqual dateTimeJsonString
      parse(dateTimeJsonString).right.flatMap(_.as[DateTime]) must beRight(dateTime)
      parse("true").right.flatMap(_.as[DateTime]) must beLeft
    }

    "provide a JsonFormat for LocalDate" in {
      val localDate = new LocalDate("2016-01-01")
      val localDateJsonString = """"2016-01-01""""

      localDate.toJson.compactPrint mustEqual localDateJsonString
      localDateJsonString.parseJson.convertTo[LocalDate] mustEqual localDate
      "true".parseJson.convertTo[LocalDate] must throwA[DeserializationException]
    }

    "provide an Encoder and Decoder for LocalDate" in {
      val localDate = new LocalDate("2016-01-01")
      val localDateJsonString = """"2016-01-01""""

      localDate.asJson.pretty(Printer.noSpaces) mustEqual localDateJsonString
      parse(localDateJsonString).right.flatMap(_.as[LocalDate]) must beRight(localDate)
      parse("true").right.flatMap(_.as[LocalDate]) must beLeft
    }

    "provide a JsonFormat for a Map as a JsArray of json objects" in {
      implicit val mapFormat: RootJsonFormat[Map[Option[Int], String]] = mapJsArrayFormat[Option[Int], String]

      val map = Map(None -> "none", Some(1) -> "one", Some(2) -> "two")
      val mapJsonString = """[{"key":null,"value":"none"},{"key":1,"value":"one"},{"key":2,"value":"two"}]"""

      map.toJson.compactPrint mustEqual mapJsonString
      mapJsonString.parseJson.convertTo[Map[Option[Int], String]] mustEqual map
      """{"key":1,"value":"one"}""".parseJson.convertTo[Map[Option[Int], String]] must throwA[DeserializationException]
      """[{"invalid":1,"value":"one"}]""".parseJson.convertTo[Map[Option[Int], String]] must throwA[DeserializationException]
      """[{"key":1,"invalid":"one"}]""".parseJson.convertTo[Map[Option[Int], String]] must throwA[DeserializationException]
    }
  }
}
