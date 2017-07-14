package eu.shiftforward.apso.json

import java.net.URI

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import org.joda.time.{ DateTime, Interval, LocalDate }

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * Provides additional JsonFormats not available in the [[spray.json.DefaultJsonProtocol]].
 */
object ExtraJsonProtocol
  extends ExtraTimeJsonProtocol
  with ExtraHttpJsonProtocol
  with ExtraMiscJsonProtocol

trait ExtraTimeJsonProtocol {
  implicit object FiniteDurationJsonFormat extends JsonFormat[FiniteDuration] {
    def write(dur: FiniteDuration) = JsObject("milliseconds" -> dur.toMillis.toJson)

    def read(json: JsValue) = {

      def tryToParseDuration(duration: String) =
        Try(Duration.fromNanos(ConfigFactory.parseString(s"d=$duration").getDuration("d").toNanos)) match {
          case Success(d) => d
          case Failure(t) => deserializationError("Expected a Number or a unit-annotated String", t)
        }

      json match {
        case JsNumber(duration) =>
          tryToParseDuration(duration.toString())

        case JsString(duration) =>
          tryToParseDuration(duration)

        case j: JsObject =>
          j.fields.headOption match {
            case Some(("milliseconds", JsNumber(milliseconds))) => milliseconds.longValue.millis
            case Some(("seconds", JsNumber(seconds))) => seconds.longValue.seconds
            case Some(("minutes", JsNumber(minutes))) => minutes.longValue.minutes
            case Some(("hours", JsNumber(hours))) => hours.longValue.hours
            case Some(("days", JsNumber(days))) => days.longValue.days
            case _ => deserializationError(
              "Expected the following units: 'milliseconds', 'seconds', 'minutes', 'hours' or 'days'.")
          }
        case _ => deserializationError("Expected either a Number, String or JSON Object!")
      }
    }
  }

  implicit object IntervalJsonFormat extends JsonFormat[Interval] {
    def write(i: Interval): JsValue =
      JsObject(
        "startMillis" -> i.getStartMillis.toJson,
        "endMillis" -> i.getEndMillis.toJson)

    def read(json: JsValue): Interval = {
      json.asJsObject.getFields("startMillis", "endMillis") match {
        case Seq(startMillis, endMillis) =>
          new Interval(startMillis.convertTo[Long], endMillis.convertTo[Long])
        case _ =>
          deserializationError(
            "One ore more fields are missing or malformed in the Interval Json. " +
              "Required fields: 'startMillis' and 'endMillis'.")
      }
    }
  }
}

trait ExtraHttpJsonProtocol {

  implicit object URIFormat extends JsonFormat[URI] {
    def write(uri: URI) = JsString(uri.toString)

    def read(json: JsValue) = json match {
      case JsString(uri) =>
        Try(new URI(uri)).getOrElse(deserializationError("Invalid URI: " + uri))
      case other => deserializationError("Expected String with URI, got: " + other)
    }
  }
}

trait ExtraMiscJsonProtocol {
  implicit object ConfigJsonFormat extends JsonFormat[Config] {
    def write(conf: Config): JsValue = conf.root.render(ConfigRenderOptions.concise()).parseJson
    def read(json: JsValue): Config = Try(ConfigFactory.parseString(json.toString)) match {
      case Success(v) => v
      case Failure(t) => deserializationError("Could not parse config: " + json, t)
    }
  }

  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    override def write(date: DateTime): JsValue = JsString(date.toString)

    override def read(json: JsValue): DateTime = json match {
      case JsString(date) => new DateTime(date)
      case _ =>
        deserializationError("The value for a 'DateTime' has an invalid type - it must be a String.")
    }
  }

  implicit object LocalDateFormat extends JsonFormat[LocalDate] {
    override def write(date: LocalDate): JsValue = date.toString.toJson

    override def read(json: JsValue): LocalDate = json match {
      case JsString(date) => new LocalDate(date)
      case _ =>
        deserializationError("The value for a 'LocalDate' has an invalid type - it must be a String.")
    }
  }
}
