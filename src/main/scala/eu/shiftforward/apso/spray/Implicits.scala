package eu.shiftforward.apso.spray

import scalaz.{ Failure, Success, Validation }
import spray.httpx.marshalling.Marshaller

/**
 * Object containing implicit classes and methods related to spray.
 */
object Implicits {

  /**
   * Implicit method that provides a marshaller for `Validation` objects. It
   * relies on the existence of marshallers for each of the possible values in
   * this validation.
   */
  implicit def validationMarshaller[E, A](implicit me: Marshaller[E], ma: Marshaller[A]) =
    Marshaller[Validation[E, A]] { (value, ctx) =>
      value match {
        case Failure(e) => me(e, ctx)
        case Success(a) => ma(a, ctx)
      }
    }
}
