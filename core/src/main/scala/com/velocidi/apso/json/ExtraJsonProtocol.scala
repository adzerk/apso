package com.velocidi.apso.json

import java.net.URI

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import io.circe._
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.parser._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ Duration => _, _ }
import spray.json.DefaultJsonProtocol._
import spray.json._
import squants.market.{ Currency, MoneyContext }

/**
 * Provides additional JsonFormats not available in the [[spray.json.DefaultJsonProtocol]].
 */
object ExtraJsonProtocol
  extends ExtraTimeJsonProtocol
  with ExtraHttpJsonProtocol
  with ExtraMiscJsonProtocol

trait ExtraTimeJsonProtocol {
  private[this] def tryToParseDuration(duration: String): Try[FiniteDuration] =
    Try(Duration.fromNanos(ConfigFactory.parseString(s"d=$duration").getDuration("d").toNanos))

  @deprecated("This will be removed in a future version.", "2019/10/23")
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
      Decoder.forProduct1[FiniteDuration, Long]("milliseconds")(_.millis) or
      Decoder.forProduct1[FiniteDuration, Long]("seconds")(_.seconds) or
      Decoder.forProduct1[FiniteDuration, Long]("minutes")(_.minutes) or
      Decoder.forProduct1[FiniteDuration, Long]("hours")(_.hours) or
      Decoder.forProduct1[FiniteDuration, Long]("days")(_.days)

  @deprecated("This will be removed in a future version.", "2019/10/23")
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
    Decoder.forProduct2[Interval, Long, Long]("startMillis", "endMillis")(new Interval(_, _))

  @deprecated("This will be removed in a future version.", "2019/10/23")
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

  @deprecated("This will be removed in a future version.", "2019/10/23")
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
  @deprecated("This will be removed in a future version.", "2019/10/23")
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

  @deprecated("This will be removed in a future version.", "2019/10/23")
  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    private val printer = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

    override def write(date: DateTime): JsValue = JsString(printer.print(date))

    override def read(json: JsValue): DateTime = json match {
      case JsString(date) => new DateTime(date)
      case _ =>
        deserializationError("The value for a 'DateTime' has an invalid type - it must be a String.")
    }
  }

  implicit val dateTimeEncoder: Encoder[DateTime] = new Encoder[DateTime] {
    private val stringEncoder = Encoder[String]
    private val printer = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

    def apply(a: DateTime): Json = stringEncoder.apply(printer.print(a))
  }
  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder[String].emapTry(v => Try(new DateTime(v)))

  @deprecated("This will be removed in a future version.", "2019/10/23")
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

  implicit def currencyDecoder(implicit moneyContext: MoneyContext): Decoder[Currency] = Decoder[String].emapTry(Currency(_))
  implicit val currencyEncoder: Encoder[Currency] = Encoder[String].contramap(_.toString)

  /**
   * Serializes a map as an array of key-value objects.
   * Note that `spray-json`'s `JsonFormat` for maps has the same signature, so if you need to use both at the same time,
   * you need to extend the `DefaultJsonProtocol` trait instead of importing it.
   *
   * @tparam K the type of the keys of the map
   * @tparam V the types of the value of the map
   * @return an instance of `RootJsonFormat` for the map
   */
  @deprecated("This will be removed in a future version.", "2019/10/23")
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

  /**
   * Serializes a map as an array of key-value objects.
   *
   * @tparam K the type of the keys of the map
   * @tparam V the types of the value of the map
   * @return an instance of `Encoder` for the map
   */
  def mapJsonArrayEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    Encoder[List[MapEntry[K, V]]].contramap(_.toList.map { case (k, v) => MapEntry(k, v) })

  /**
   * Deserializes a map from array of key-value objects.
   *
   * @tparam K the type of the keys of the map
   * @tparam V the types of the value of the map
   * @return an instance of `Decoder` for the map
   */
  def mapJsonArrayDecoder[K: Decoder, V: Decoder]: Decoder[Map[K, V]] =
    Decoder[List[MapEntry[K, V]]].map(_.flatMap(me => Some(me.key, me.value)).toMap)

  private case class MapEntry[K, V](key: K, value: V)
  private implicit def mapEntryEncoder[K: Encoder, V: Encoder]: Encoder[MapEntry[K, V]] = deriveEncoder[MapEntry[K, V]]
  private implicit def mapEntryDecoder[K: Decoder, V: Decoder]: Decoder[MapEntry[K, V]] =
    deriveDecoder[MapEntry[K, V]].validate(_.keys.exists(k => k.exists(_ == "key") && k.exists(_ == "value")), "Expected a json object with 'key' and 'value' as keys")
}
