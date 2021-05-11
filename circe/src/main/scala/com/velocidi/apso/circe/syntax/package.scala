package com.velocidi.apso.circe

import scala.util.Try

import io.circe.Decoder

package object syntax {
  implicit class DecoderExtras[A](val decoder: Decoder[A]) extends AnyVal {

    /** Performs similarly to `emapTry` but in case of failure it exclusively uses the Throwable's message
      * to create the DecodingFailure message whereas `emapTry` would use the message+stacktrace.
      * Related issue: https://github.com/circe/circe/issues/306
      *
      * @param f a function returning a Try of value
      */
    def emapPrettyTry[B](f: A => Try[B]): Decoder[B] =
      decoder.emap(f(_).toEither.left.map(_.getMessage))
  }
}
