package eu.shiftforward.apso.json

import java.net.URI

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import io.circe._
import io.circe.parser._
import org.joda.time.{ DateTime, Interval, LocalDate, Period }
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
  def tryToParseDuration(duration: String): Try[FiniteDuration] =
    Try(Duration.fromNanos(ConfigFactory.parseString(s"d=$duration").getDuration("d").toNanos))

  implicit object FiniteDurationJsonFormat extends JsonFormat[FiniteDuration] {
    def write(dur: FiniteDuration) = JsObject("milliseconds" -> dur.toMillis.toJson)

    def read(json: JsValue) = {
      json match {
        case JsNumber(duration) =>
          tryToParseDuration(duration.toString()) match {
            case Success(d) => d
            case Failure(t) => deserializationError("Expected a Number or a unit-annotated String", t)
          }

        case JsString(duration) =>
          tryToParseDuration(duration) match {
            case Success(d) => d
            case Failure(t) => deserializationError("Expected a Number or a unit-annotated String", t)
          }

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

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.forProduct1("milliseconds")(_.toMillis)
  implicit val finiteDurationDecoder: Decoder[FiniteDuration] =
    Decoder[Long].emapTry(v => tryToParseDuration(v.toString)) or
      Decoder[String].emapTry(v => tryToParseDuration(v)) or
      Decoder.forProduct1[Long, FiniteDuration]("milliseconds")(_.millis) or
      Decoder.forProduct1[Long, FiniteDuration]("seconds")(_.seconds) or
      Decoder.forProduct1[Long, FiniteDuration]("minutes")(_.minutes) or
      Decoder.forProduct1[Long, FiniteDuration]("hours")(_.hours) or
      Decoder.forProduct1[Long, FiniteDuration]("days")(_.days)

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

  implicit val intervalEncoder: Encoder[Interval] =
    Encoder.forProduct2("startMillis", "endMillis")(int => (int.getStartMillis, int.getEndMillis))
  implicit val intervalDecoder: Decoder[Interval] =
    Decoder.forProduct2[Long, Long, Interval]("startMillis", "endMillis")(new Interval(_, _))

  implicit object PeriodJsonFormat extends JsonFormat[Period] {
    def write(p: Period): JsValue = p.toString.toJson

    def read(v: JsValue): Period =
      v match {
        case JsString(v) =>
          Try(new Period(v)).getOrElse(deserializationError(s"Could not parse Period: $v"))
        case other => deserializationError(s"Expected String with Period, got: $other")
      }
  }

  implicit val periodEncoder: Encoder[Period] =
    Encoder[String].contramap(_.toString)
  implicit val periodDecoder: Decoder[Period] =
    Decoder[String].emapTry(v => Try(new Period(v)))
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

  implicit val uriEncoder: Encoder[URI] =
    Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[URI] =
    Decoder[String].emapTry(v => Try(new URI(v)))
}

trait ExtraMiscJsonProtocol {
  implicit object ConfigJsonFormat extends JsonFormat[Config] {
    def write(conf: Config): JsValue = conf.root.render(ConfigRenderOptions.concise()).parseJson
    def read(json: JsValue): Config = Try(ConfigFactory.parseString(json.toString)) match {
      case Success(v) => v
      case Failure(t) => deserializationError("Could not parse config: " + json, t)
    }
  }

  implicit val configEncoder: Encoder[Config] =
    Encoder[Json].contramap(conf => parse(conf.root.render(ConfigRenderOptions.concise())).fold(throw _, identity))
  implicit val configDecoder: Decoder[Config] =
    Decoder[Json].emapTry(json => Try(ConfigFactory.parseString(json.toString)))

  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    override def write(date: DateTime): JsValue = JsString(date.toString)

    override def read(json: JsValue): DateTime = json match {
      case JsString(date) => new DateTime(date)
      case _ =>
        deserializationError("The value for a 'DateTime' has an invalid type - it must be a String.")
    }
  }

  implicit val dateTimeEncoder: Encoder[DateTime] =
    Encoder[String].contramap(_.toString)
  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder[String].emapTry(v => Try(new DateTime(v)))

  implicit object LocalDateFormat extends JsonFormat[LocalDate] {
    override def write(date: LocalDate): JsValue = date.toString.toJson

    override def read(json: JsValue): LocalDate = json match {
      case JsString(date) => new LocalDate(date)
      case _ =>
        deserializationError("The value for a 'LocalDate' has an invalid type - it must be a String.")
    }
  }

  implicit val localDateEncoder: Encoder[LocalDate] =
    Encoder[String].contramap(_.toString)
  implicit val localDateDecoder: Decoder[LocalDate] =
    Decoder[String].emapTry(v => Try(new LocalDate(v)))

  /**
   * Serializes a map as an array of key-value objects.
   * Note that `spray-json`'s `JsonFormat` for maps has the same signature, so if you need to use both at the same time,
   * you need to extend the `DefaultJsonProtocol` trait instead of importing it.
   *
   * @tparam K the type of the keys of the map
   * @tparam V the types of the value of the map
   * @return an instance of `RootJsonFormat` for the map
   */
  def mapJsArrayFormat[K: JsonFormat, V: JsonFormat]: RootJsonFormat[Map[K, V]] = new RootJsonFormat[Map[K, V]] {
    def write(obj: Map[K, V]): JsValue = obj.map(o => JsObject(Map("key" -> o._1.toJson, "value" -> o._2.toJson))).toJson

    def read(json: JsValue): Map[K, V] = json.convertTo[JsArray].elements.foldLeft(Map[K, V]()) { (map, e) =>
      e.asJsObject.getFields("key", "value") match {
        case Seq(k, v) =>
          map ++ Map(k.convertTo[K] -> v.convertTo[V])
        case other => deserializationError(s"Expected a json object with 'key' and 'value' as keys, got: $other")
      }
    }
  }
}
