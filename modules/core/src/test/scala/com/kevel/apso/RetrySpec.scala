package com.kevel.apso

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Failure

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class RetrySpec(implicit ee: ExecutionEnv) extends Specification {

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

    "don't retry a doomed function throwing a Fatal exception" in {
      var attempts = 0
      val retries = 10

      val f =
        try {
          Retry.retry[Any](retries) {
            attempts = attempts + 1
            throw new OutOfMemoryError("Doomed")
          }
        } catch {
          case _: OutOfMemoryError =>
            Failure(new RuntimeException("Failed previously with out of memory!"))
        }

      eventually(f must beAFailedTry.like { case ex: RuntimeException =>
        ex.getMessage must beEqualTo("Failed previously with out of memory!")
      })

      attempts must beEqualTo(1) // 1 attempt
    }

    "compute an exponentially growing back-off delay" in {
      val delays =
        (0 until 4).map(attempt => Retry.exponentialBackOffDelay(attempt, 100.millis, jitter = 0.0))

      delays.map(_.toMillis) must beEqualTo(Seq(100L, 200L, 400L, 800L))
    }

    "cap the back-off delay with the given maximum" in {
      val delays =
        (0 until 4).map(attempt => Retry.exponentialBackOffDelay(attempt, 100.millis, Some(250.millis), jitter = 0.0))

      delays.map(_.toMillis) must beEqualTo(Seq(100L, 200L, 250L, 250L))
    }

    "grow the back-off delay by the given factor" in {
      val delays =
        (0 until 4).map(attempt => Retry.exponentialBackOffDelay(attempt, 100.millis, factor = 3.0, jitter = 0.0))

      delays.map(_.toMillis) must beEqualTo(Seq(100L, 300L, 900L, 2_700L))
    }

    "randomize the whole back-off delay with a full jitter" in {
      val delays = (1 to 100).map(_ => Retry.exponentialBackOffDelay(0, 100.millis, jitter = 1.0).toMillis)

      forall(delays)(d => d must beBetween(0L, 100L))
      delays.distinct.size must be_>(1) // the jitter is random, so the delays must not all be the same
      delays.min must be_<(50L) // the delays spread over the whole range, instead of clustering near the top
    }

    "randomize only the given fraction of the back-off delay" in {
      val delays = (1 to 100).map(_ => Retry.exponentialBackOffDelay(0, 100.millis, jitter = 0.25).toMillis)

      forall(delays)(d => d must beBetween(75L, 100L))
      delays.distinct.size must be_>(1)
    }

    "clamp the jitter to a fraction of the back-off delay" in {
      Retry.exponentialBackOffDelay(0, 100.millis, jitter = -1.0).toMillis must beEqualTo(100L)

      val delays = (1 to 20).map(_ => Retry.exponentialBackOffDelay(0, 100.millis, jitter = 2.0).toMillis)
      forall(delays)(d => d must beBetween(0L, 100L))
    }

    "retry a given function with exponential back-off a number of times" in {
      var attempts = 0

      val f = Retry.exponentialBackOff(10, 1.milli, jitter = 0.0) {
        attempts = attempts + 1
        if (attempts <= 3) throw new RuntimeException("Doomed")
        else attempts
      }

      f must beSuccessfulTry(4)
      attempts must beEqualTo(4)
    }

    "retry a doomed function with exponential back-off until it fails" in {
      var attempts = 0
      val retries = 5

      val f = Retry.exponentialBackOff[Any](retries, 1.milli, jitter = 0.0) {
        attempts = attempts + 1
        throw new RuntimeException("Doomed")
      }

      f must beAFailedTry
      attempts must beEqualTo(1 + retries) // 1 attempt + 5 retries
    }

    "not retry a failure rejected by the given predicate" in {
      var attempts = 0

      val f = Retry.exponentialBackOff[Any](
        10,
        1.milli,
        jitter = 0.0,
        retryWhen = _.getMessage != "Fatal"
      ) {
        attempts = attempts + 1
        throw new RuntimeException("Fatal")
      }

      f must beAFailedTry
      attempts must beEqualTo(1) // 1 attempt, no retries
    }

    "evaluate the given predicate once per failure" in {
      var seen = 0

      Retry.exponentialBackOff[Any](
        2,
        1.milli,
        jitter = 0.0,
        retryWhen = { _ =>
          seen += 1
          true
        }
      ) {
        throw new RuntimeException("Doomed")
      }

      seen must beEqualTo(3) // 1 attempt + 2 retries, all failing
    }

    "report each retry to the given function" in {
      var retries = List.empty[(String, Long, Int)]

      Retry.exponentialBackOff[Any](
        3,
        100.millis,
        jitter = 0.0,
        onRetry = (ex, delay, remaining) => retries = retries :+ (ex.getMessage, delay.toMillis, remaining)
      ) {
        throw new RuntimeException("Doomed")
      }

      // one report per retry, never for the final failed attempt
      retries must beEqualTo(List(("Doomed", 100L, 3), ("Doomed", 200L, 2), ("Doomed", 400L, 1)))
    }

    "report reaching the maximum retries with the failure that exhausted them" in {
      var reached = List.empty[String]

      val f = Retry.exponentialBackOff[Any](
        2,
        1.milli,
        jitter = 0.0,
        onMaxRetriesReached = ex => reached = reached :+ ex.getMessage
      ) {
        throw new RuntimeException("Doomed")
      }

      f must beAFailedTry
      reached must beEqualTo(List("Doomed")) // reported once, after the last attempt failed
    }

    "not report reaching the maximum retries when the failure is rejected by the predicate" in {
      var attempts = 0
      var retried = 0
      var reached = 0

      val f = Retry.exponentialBackOff[Any](
        5,
        1.milli,
        jitter = 0.0,
        retryWhen = _ => false,
        onRetry = (_, _, _) => retried = retried + 1,
        onMaxRetriesReached = _ => reached = reached + 1
      ) {
        attempts = attempts + 1
        throw new RuntimeException("Fatal")
      }

      f must beAFailedTry
      attempts must beEqualTo(1)
      retried must beEqualTo(0)
      reached must beEqualTo(0) // the retries were never exhausted, the failure was rejected upfront
    }

    "not report reaching the maximum retries when a retry succeeds" in {
      var attempts = 0
      var reached = 0

      val f = Retry.exponentialBackOff(
        5,
        1.milli,
        jitter = 0.0,
        onMaxRetriesReached = _ => reached = reached + 1
      ) {
        attempts = attempts + 1
        if (attempts < 3) throw new RuntimeException("Doomed") else attempts
      }

      f must beSuccessfulTry(3)
      reached must beEqualTo(0)
    }

    "not report anything when the function succeeds on the first attempt" in {
      var retried = 0
      var reached = 0

      val f = Retry.exponentialBackOff(
        5,
        1.milli,
        jitter = 0.0,
        onRetry = (_, _, _) => retried = retried + 1,
        onMaxRetriesReached = _ => reached = reached + 1
      )(42)

      f must beSuccessfulTry(42)
      retried must beEqualTo(0)
      reached must beEqualTo(0)
    }

    "make a single attempt when no retries are allowed" in {
      var attempts = 0
      var retried = 0
      var reached = 0

      val f = Retry.exponentialBackOff[Any](
        0,
        1.milli,
        jitter = 0.0,
        onRetry = (_, _, _) => retried = retried + 1,
        onMaxRetriesReached = _ => reached = reached + 1
      ) {
        attempts = attempts + 1
        throw new RuntimeException("Doomed")
      }

      f must beAFailedTry
      attempts must beEqualTo(1)
      retried must beEqualTo(0)
      reached must beEqualTo(1)
    }
  }
}
