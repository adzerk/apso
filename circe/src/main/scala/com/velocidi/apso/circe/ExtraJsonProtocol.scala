package com.velocidi.apso.circe

import java.net.URI

import scala.concurrent.duration._
import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{Duration => _, _}
import squants.market.{Currency, MoneyContext}

import com.velocidi.apso.circe.syntax._

/** Provides Encoders and Decoders for some relevant types.
  */
object ExtraJsonProtocol extends ExtraTimeJsonProtocol with ExtraHttpJsonProtocol with ExtraMiscJsonProtocol

trait ExtraTimeJsonProtocol {
  private[this] def tryToParseDuration(duration: String): Try[FiniteDuration] =
    Try(Duration.fromNanos(ConfigFactory.parseString(s"d=$duration").getDuration("d").toNanos))

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.forProduct1("milliseconds")(_.toMillis)
  implicit val finiteDurationDecoder: Decoder[FiniteDuration] =
    Decoder[Long].emapPrettyTry(v => tryToParseDuration(v.toString)) or
      Decoder[String].emapPrettyTry(v => tryToParseDuration(v)) or
      Decoder.forProduct1[FiniteDuration, Long]("milliseconds")(_.millis) or
      Decoder.forProduct1[FiniteDuration, Long]("seconds")(_.seconds) or
      Decoder.forProduct1[FiniteDuration, Long]("minutes")(_.minutes) or
      Decoder.forProduct1[FiniteDuration, Long]("hours")(_.hours) or
      Decoder.forProduct1[FiniteDuration, Long]("days")(_.days)

  implicit val intervalEncoder: Encoder[Interval] =
    Encoder.forProduct2("startMillis", "endMillis")(int => (int.getStartMillis, int.getEndMillis))
  implicit val intervalDecoder: Decoder[Interval] =
    Decoder.forProduct2[Interval, Long, Long]("startMillis", "endMillis")(new Interval(_, _))

  implicit val periodEncoder: Encoder[Period] =
    Encoder[String].contramap(_.toString)
  implicit val periodDecoder: Decoder[Period] =
    Decoder[String].emapPrettyTry(v => Try(new Period(v)))

  implicit val dateTimeEncoder: Encoder[DateTime] = new Encoder[DateTime] {
    private val stringEncoder = Encoder[String]
    private val printer = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

    def apply(a: DateTime): Json = stringEncoder.apply(printer.print(a))
  }
  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder[String].emapPrettyTry(v => Try(DateTime.parse(v).toDateTime(DateTimeZone.UTC)))

  implicit val localDateEncoder: Encoder[LocalDate] =
    Encoder[String].contramap(_.toString)
  implicit val localDateDecoder: Decoder[LocalDate] =
    Decoder[String].emapPrettyTry(v => Try(new LocalDate(v)))
}

object ExtraTimeJsonProtocol extends ExtraTimeJsonProtocol

trait ExtraHttpJsonProtocol {
  implicit val uriEncoder: Encoder[URI] =
    Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[URI] =
    Decoder[String].emapPrettyTry(v => Try(new URI(v)))
}

object ExtraHttpJsonProtocol extends ExtraHttpJsonProtocol

trait ExtraMiscJsonProtocol {
  implicit val configEncoder: Encoder[Config] =
    Encoder[Json].contramap(conf => parse(conf.root.render(ConfigRenderOptions.concise())).fold(throw _, identity))
  implicit val configDecoder: Decoder[Config] =
    Decoder[Json].emapPrettyTry(json => Try(ConfigFactory.parseString(json.toString)))

  implicit def currencyDecoder(implicit moneyContext: MoneyContext): Decoder[Currency] =
    Decoder[String].emapPrettyTry(s => Currency(s.toUpperCase))
  implicit val currencyEncoder: Encoder[Currency] = Encoder[String].contramap(_.toString)

  /** Encodes a map as an array of key-value objects.
    *
    * @tparam K
    *   the type of the keys of the map
    * @tparam V
    *   the types of the value of the map
    * @return
    *   an instance of `Encoder` for the map
    */
  def mapJsonArrayEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    Encoder[List[MapEntry[K, V]]].contramap(_.toList.map { case (k, v) => MapEntry(k, v) })

  /** Decodes a map from an array of key-value objects.
    *
    * @tparam K
    *   the type of the keys of the map
    * @tparam V
    *   the types of the value of the map
    * @return
    *   an instance of `Decoder` for the map
    */
  def mapJsonArrayDecoder[K: Decoder, V: Decoder]: Decoder[Map[K, V]] =
    Decoder[List[MapEntry[K, V]]].map(_.flatMap(me => Some(me.key, me.value)).toMap)

  private case class MapEntry[K, V](key: K, value: V)
  private implicit def mapEntryEncoder[K: Encoder, V: Encoder]: Encoder[MapEntry[K, V]] = deriveEncoder[MapEntry[K, V]]
  private implicit def mapEntryDecoder[K: Decoder, V: Decoder]: Decoder[MapEntry[K, V]] =
    deriveDecoder[MapEntry[K, V]].validate(
      _.keys.exists(k => k.exists(_ == "key") && k.exists(_ == "value")),
      "Expected a json object with 'key' and 'value' as keys"
    )
}

object ExtraMiscJsonProtocol extends ExtraMiscJsonProtocol
