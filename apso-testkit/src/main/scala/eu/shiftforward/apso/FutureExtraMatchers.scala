package eu.shiftforward.apso

import org.specs2.matcher._
import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

trait FutureExtraMatchers extends NoTimeConversions { this: SpecificationLike =>

  implicit class RichAwaitable[T](val awaitable: Awaitable[T]) {
    def get = Await.result(awaitable, 1.second)
    def await(timeout: Duration) = Await.result(awaitable, timeout)
  }

  implicit class RichFutureExtraMatcher[T](m: Matcher[T]) {
    /**
     * @return a matcher that needs to eventually match, after a given number of retries.
     */
    def eventually(retries: Int): Matcher[T] = EventuallyMatchers.eventually(m, retries, 100.milliseconds)
  }

  def beEventually[T](f: T => MatchResult[_]): Matcher[T] =
    (f: Matcher[T]).eventually(40, 200.milliseconds)

  def beEventually[T](retries: Int, sleep: FiniteDuration)(f: T => MatchResult[_]): Matcher[T] =
    (f: Matcher[T]).eventually(retries, sleep)
}
