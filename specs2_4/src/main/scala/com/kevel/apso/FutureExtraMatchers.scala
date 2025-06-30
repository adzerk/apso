package com.kevel.apso

import scala.concurrent._
import scala.concurrent.duration._

import org.specs2.execute.AsResult
import org.specs2.matcher._
import org.specs2.mutable.SpecificationLike

trait FutureExtraMatchers { this: SpecificationLike =>

  implicit class RichAwaitable[T](val awaitable: Awaitable[T]) {
    def get = Await.result(awaitable, 1.second)
    def await(timeout: Duration) = Await.result(awaitable, timeout)
  }

  implicit class RichFutureExtraMatcher[T: AsResult](m: => T) {

    /** @return
      *   a matcher that needs to eventually match, after a given number of retries.
      */
    def eventually(retries: Int): T = EventuallyMatchers.eventually(retries, 100.milliseconds)(m)
  }

  def beEventually[T: AsResult](f: => T): T =
    eventually(40, 200.milliseconds)(f)

  def beEventually[T: AsResult](retries: Int, sleep: FiniteDuration)(f: => T): T =
    eventually(retries, sleep)(f)
}
