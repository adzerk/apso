package eu.shiftforward.apso

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import scala.concurrent.Future

class RetrySpec(implicit ee: ExecutionEnv) extends Specification with FutureExtraMatchers {

  "A Retry mechanism" should {

    "retry a future number of times" in {
      var attempts = 0

      Retry.retryFuture(10) {
        Future {
          attempts = attempts + 1
          attempts
        }.filter(_ > 3)
      }

      attempts must beEqualTo(4).eventually
    }

    "retry a doomed future a number of times until it fails" in {
      var attempts = 0
      val retries = 10

      val f = Retry.retryFuture[Any](retries) {
        Future {
          attempts = attempts + 1
          throw new RuntimeException("Doomed")
        }
      }

      eventually {
        f must throwAn[RuntimeException].await
      }

      attempts must beEqualTo(1 + retries) // 1 attempt + 10 retries
    }

    "retry a given function a number of times" in {
      var attempts = 0

      Retry.retry(10) {
        attempts = attempts + 1
        if (attempts <= 3) throw new RuntimeException("Doomed")
        else attempts
      }

      attempts must beEqualTo(4).eventually
    }

    "retry a doomed function a number of times until it fails" in {
      var attempts = 0
      val retries = 10

      val f = Retry.retry[Any](retries) {
        attempts = attempts + 1
        throw new RuntimeException("Doomed")
      }

      eventually(f must beAFailedTry)

      attempts must beEqualTo(1 + retries) // 1 attempt + 10 retries
    }
  }
}
