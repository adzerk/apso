package eu.shiftforward.apso

import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrySpec extends Specification with FutureExtraMatchers {

  "A Retry mechanism" should {

    "retry a future number of times" in {
      var attempts = 0

      Retry(10) {
        Future {
          attempts = attempts + 1
          attempts
        }.filter(_ > 3)
      }.await(1.second)

      attempts must beEqualTo(4)
    }

    "retry a doomed future a number of times until it fails" in {
      var attempts = 0

      Retry[Any](10) {
        Future {
          attempts = attempts + 1
          throw new RuntimeException("Doomed")
        }
      }.await(2.second) must throwAn[RuntimeException]

      attempts must beEqualTo(11) // 1 attempt + 10 retries
    }
  }
}
