package eu.shiftforward.apso.spray

import scalaz.{Failure, Success, Validation}
import spray.httpx.marshalling.Marshaller

object Implicits {
  implicit def validationMarshaller[E, A](implicit me: Marshaller[E], ma: Marshaller[A]) =
    Marshaller[Validation[E, A]] { (value, ctx) =>
      value match {
        case Failure(e) => me(e, ctx)
        case Success(a) => ma(a, ctx)
      }
    }
}
