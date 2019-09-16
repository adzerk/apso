package com.velocidi.apso.currencies

import scala.util.{ Failure, Success, Try }

import io.circe.generic.semiauto.deriveCodec
import io.circe.{ Codec, parser }

import com.velocidi.apso.io.FileDescriptor

case class CurrencyRate(from: String, to: String, rate: BigDecimal)

object CurrencyRate {

  implicit val codec: Codec[CurrencyRate] = deriveCodec[CurrencyRate]

  def fromFileDescriptor(fileDescriptor: FileDescriptor): Try[Set[CurrencyRate]] = {
    Try(fileDescriptor.lines.mkString).flatMap(parser.decode[Set[CurrencyRate]](_).fold(Failure(_), Success(_)))
  }
}
