package com.velocidi.apso.currencies

import scala.util.Try

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder, parser }

import com.velocidi.apso.io.FileDescriptor

case class CurrencyRate(from: String, to: String, rate: BigDecimal)

object CurrencyRate {

  implicit val decoder: Decoder[CurrencyRate] = deriveDecoder[CurrencyRate]
  implicit val encoder: Encoder[CurrencyRate] = deriveEncoder[CurrencyRate]

  def fromFileDescriptor(fileDescriptor: FileDescriptor): Try[Set[CurrencyRate]] = {
    Try(fileDescriptor.lines.mkString).flatMap(parser.decode[Set[CurrencyRate]](_).toTry)
  }
}
