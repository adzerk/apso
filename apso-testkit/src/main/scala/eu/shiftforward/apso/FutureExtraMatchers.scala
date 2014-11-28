package eu.shiftforward.apso

import org.specs2.matcher.{ MatchResult, Matcher }
import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

trait FutureExtraMatchers extends NoTimeConversions { this: SpecificationLike =>

  implicit class RichAwaitable[T](val awaitable: Awaitable[T]) {
    def get = Await.result(awaitable, 1.second)
    def await(timeout: Duration) = Await.result(awaitable, timeout)
  }

  def beEventually[T](f: T => MatchResult[_]): Matcher[T] =
    (f: Matcher[T]).eventually(40, 200.milliseconds)

  def beEventually[T](retries: Int, sleep: FiniteDuration)(f: T => MatchResult[_]): Matcher[T] =
    (f: Matcher[T]).eventually(retries, sleep)
}
