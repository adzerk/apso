package eu.shiftforward.apso.scalaz

import scala.util.{ Failure => TFailure, Try, Success => TSuccess }
import scalaz.{ Failure, Success, Validation }

/**
 * Object containing implicit classes and methods related to scalaz.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
object Implicits {

  /**
   * Implicit class that provides new methods for `Try` objects.
   * @param t the `Try` object to which the new methods are provided.
   */
  final implicit class ApsoTry[T](val t: Try[T]) extends AnyVal {
    def toValidation: Validation[Throwable, T] = t
  }

  /**
   * Implicit method that provides new methods for `Try` objects.
   * @param t the `Try` object to which the new methods are provided.
   * @todo delete this in favor of the implicit class with the same effect.
   */
  implicit def try2validation[T](t: Try[T]): Validation[Throwable, T] = t match {
    case TSuccess(t) => Success(t)
    case TFailure(t) => Failure(t)
  }
}
