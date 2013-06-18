package eu.shiftforward.apso.scalaz

import scala.util.{ Failure => TFailure, Try, Success => TSuccess }
import scalaz.{ Failure, Success, Validation }

object Implicits {
  final implicit class ApsoTry[T](val t: Try[T]) extends AnyVal {
    def toValidation: Validation[Throwable, T] = t
  }

  implicit def try2validation[T](t: Try[T]): Validation[Throwable, T] = t match {
    case TSuccess(t) => Success(t)
    case TFailure(t) => Failure(t)
  }
}
