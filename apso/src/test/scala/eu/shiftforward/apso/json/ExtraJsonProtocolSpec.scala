package eu.shiftforward.apso.json

import java.net.URI

import scala.concurrent.duration._

import com.typesafe.config.{ Config, ConfigFactory }
import org.joda.time.{ DateTime, Interval, LocalDate }
import org.specs2.mutable.Specification
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

    "provide a JsonFormat for Interval" in {
      val interval = new Interval(1000, 2000)
      val intervalJsonString = """{"startMillis":1000,"endMillis":2000}"""

      interval.toJson.compactPrint mustEqual intervalJsonString
      intervalJsonString.parseJson.convertTo[Interval] mustEqual interval
      """{"invalidObject":true}""".parseJson.convertTo[Interval] must throwA[DeserializationException]
    }

    "provide a JsonFormat for URI" in {
      val uri = new URI("http://example.com")
      val uriJsonString = """"http://example.com""""

      uri.toJson.compactPrint mustEqual uriJsonString
      uriJsonString.parseJson.convertTo[URI] mustEqual uri
      "true".parseJson.convertTo[URI] must throwA[DeserializationException]
      """"{invalidUri}"""".parseJson.convertTo[URI] must throwA[DeserializationException]
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

    "provide a JsonFormat for DateTime" in {
      val dateTime = new DateTime("2016-01-01")
      val dateTimeJsonString = """"2016-01-01T00:00:00.000Z""""

      dateTime.toJson.compactPrint mustEqual dateTimeJsonString
      dateTimeJsonString.parseJson.convertTo[DateTime] mustEqual dateTime
      "true".parseJson.convertTo[DateTime] must throwA[DeserializationException]
    }

    "provide a JsonFormat for LocalDate" in {
      val localDate = new LocalDate("2016-01-01")
      val localDateJsonString = """"2016-01-01""""

      localDate.toJson.compactPrint mustEqual localDateJsonString
      localDateJsonString.parseJson.convertTo[LocalDate] mustEqual localDate
      "true".parseJson.convertTo[LocalDate] must throwA[DeserializationException]
    }
  }
}
