package eu.shiftforward.apso.scalaz

import scala.util.{ Failure => ScalaFailure, Success => ScalaSuccess }
import scalaz.{ Failure => ZFailure, Success => ZSuccess }

import org.specs2.mutable._

import eu.shiftforward.apso.scalaz.Implicits._

@deprecated("This will be removed in a future version", "2017/07/13")
class ImplicitsSpec extends Specification {
  "The ApsoTry" should {
    "be convertible to a Scalaz validation" in {
      ScalaSuccess(1).toValidation must beEqualTo(ZSuccess(1))
      val exception = new Exception("test")
      ScalaFailure(exception).toValidation must beEqualTo(ZFailure(exception))
    }
  }
}
