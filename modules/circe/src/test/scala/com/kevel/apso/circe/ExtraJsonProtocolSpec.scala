package com.kevel.apso.circe

import java.net.URI

import scala.concurrent.duration.*

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.*
import io.circe.literal.*
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.{DateTime, Interval, LocalDate, Period}
import org.specs2.mutable.Specification
import squants.market.*

class ExtraJsonProtocolSpec extends Specification {

  "The object ExtraJsonProtocol" should {
    import ExtraJsonProtocol.*

    "provide an Encoder and Decoder for FiniteDuration" in {
      val duration = 10.seconds
      val durationJson = json"""{"milliseconds":10000}"""

      duration.asJson mustEqual durationJson
      durationJson.as[FiniteDuration] must beRight(duration)

      decode[FiniteDuration]("""{"seconds": 2}""") must beRight(2.seconds)
      decode[FiniteDuration]("""{"minutes": 2}""") must beRight(2.minutes)
      decode[FiniteDuration]("""{"hours":   2}""") must beRight(2.hours)
      decode[FiniteDuration]("""{"days":    2}""") must beRight(2.days)
      decode[FiniteDuration]("""{"meters":  2}""") must beLeft

      decode[FiniteDuration]("""2""") must beRight(2.milliseconds)
      decode[FiniteDuration](""""2s"""") must beRight(2.seconds)
      decode[FiniteDuration](""""2m"""") must beRight(2.minutes)
      decode[FiniteDuration](""""2h"""") must beRight(2.hours)
      decode[FiniteDuration](""""2d"""") must beRight(2.days)
      decode[FiniteDuration](""""garbagio"""") must beLeft

      decode[FiniteDuration]("true") must beLeft
    }

    "provide an Encoder and Decoder for Interval" in {
      val interval = new Interval(1000, 2000)
      val intervalJson = json"""{"startMillis":1000,"endMillis":2000}"""

      interval.asJson mustEqual intervalJson
      intervalJson.as[Interval] must beRight(interval)
    }

    "provide an Encoder and Decoder for Period" in {
      val pStrings = Seq("P1D", "P1M2D", "P1M2DT10H30M")
      pStrings.forall { s =>
        val period = new Period(s)
        period.asJson.printWith(Printer.noSpaces) mustEqual s""""$s""""
        decode[Period](s""""$s"""") must beRight(period)
      }

      decode[Period](""""garbage"""") must beLeft
      decode[Period](""""PXD"""") must beLeft
    }

    "provide an Encoder and Decoder for URI" in {
      val uri = new URI("http://example.com")
      val uriJsonString = json""""http://example.com""""

      uri.asJson mustEqual uriJsonString
      uriJsonString.as[URI] must beRight(uri)
      decode[URI]("true") must beLeft
      json""""{invalidUri}"""".as[URI] must beLeft
    }

    "provide an Encoder and Decoder for Config" in {
      val config = ConfigFactory.parseString("""
                                               |a = 123
                                               |b {
                                               |  x = 1d
                                               |  y = "string"
                                               |}
                                             """.stripMargin)
      val configJsonString = json"""{"a":123,"b":{"x":"1d","y":"string"}}"""

      config.asJson mustEqual configJsonString
      configJsonString.as[Config] must beRight(config)
      decode[Config]("true") must beLeft
    }

    "provide an Encoder and Decoder for DateTime" in {
      val dateTime = DateTime.parse("2016-01-01T00:00:00.000Z")
      val dateTimeJsonString = json""""2016-01-01T00:00:00.000Z""""

      dateTime.asJson mustEqual dateTimeJsonString
      dateTimeJsonString.as[DateTime] must beRight(dateTime)
      decode[DateTime]("true") must beLeft
    }

    "provide an Encoder and Decoder for LocalDate" in {
      val localDate = new LocalDate("2016-01-01")
      val localDateJsonString = """"2016-01-01""""

      localDate.asJson.printWith(Printer.noSpaces) mustEqual localDateJsonString
      decode[LocalDate](localDateJsonString) must beRight(localDate)
      decode[LocalDate]("true") must beLeft
    }

    "provide an Encoder and Decoder for Currency" in {
      implicit val moneyContext: MoneyContext = MoneyContext(EUR, defaultCurrencySet, Nil)
      val usd: Currency = USD

      usd.asJson.noSpaces mustEqual "\"USD\""
      decode[Currency]("\"USD\"") must beRight(usd)
      decode[Currency]("\"usd\"") must beRight(usd)
      decode[Currency]("\"EU\"") must beLeft
    }

    "provide an Encoder and Decoder for a Map as an array of json objects" in {
      implicit val mapEncoder: Encoder[Map[Option[Int], String]] = mapJsonArrayEncoder[Option[Int], String]
      implicit val mapDecoder: Decoder[Map[Option[Int], String]] = mapJsonArrayDecoder[Option[Int], String]

      val map = Map(None -> "none", Some(1) -> "one", Some(2) -> "two")
      val mapJson = json"""[{"key":null,"value":"none"},{"key":1,"value":"one"},{"key":2,"value":"two"}]"""

      map.asJson mustEqual mapJson
      mapJson.as[Map[Option[Int], String]] must beRight(map)
      json"""{"key":1,"value":"one"}""".as[Map[Option[Int], String]] must beLeft
      json"""[{"invalid":1,"value":"one"}]""".as[Map[Option[Int], String]] must beLeft
      json"""[{"key":1,"invalid":"one"}]""".as[Map[Option[Int], String]] must beLeft
    }
  }
}
