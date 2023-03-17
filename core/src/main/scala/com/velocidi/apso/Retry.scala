package com.velocidi.apso

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/** Utility object with retry mechanisms.
  */
object Retry {

  private[this] final def retryFuture[T](maxRetries: Int, inBetweenSleep: Option[FiniteDuration])(
      f: => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = {
    maxRetries match {
      case 0 =>
        f
      case _ =>
        f recoverWith {
          case NonFatal(
                _
              ) => // it would be indifferent to use a Throwable here because Futures don't catch Fatal exceptions
            inBetweenSleep.foreach(d => Thread.sleep(d.toMillis))
            retryFuture[T](maxRetries - 1, inBetweenSleep)(f)
        }
    }
  }

  /** Tries to perform a Future[T] until it succeeds or until maximum retries is reached.
    *
    * @param maxRetries
    *   the number of retries, 10 by default
    * @param inBetweenSleep
    *   the duration to wait between attempts, 100 milliseconds by default
    * @param f
    *   the function that returns the Future[T]
    * @param ec
    *   the implicit execution context
    * @tparam T
    *   the type of what the future completes with
    * @return
    *   the Future[T]
    */
  def retryFuture[T](maxRetries: Int = 10, inBetweenSleep: FiniteDuration = 100.millis)(
      f: => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = retryFuture(maxRetries, Option(inBetweenSleep))(f)

  private[this] final def retry[T](maxRetries: Int, inBetweenSleep: Option[FiniteDuration])(f: => T): Try[T] = {
    maxRetries match {
      case 0 => Try(f)
      case _ =>
        Try(f) match {
          case res @ Success(_) => res
          case Failure(_) =>
            inBetweenSleep.foreach(d => Thread.sleep(d.toMillis))
            retry[T](maxRetries - 1, inBetweenSleep)(f)
        }
    }
  }

  /** Tries to perform a function `f` until it succeeds or until maximum retries is reached.
    *
    * @param maxRetries
    *   the number of retries, 10 by default
    * @param inBetweenSleep
    *   the duration to wait between attempts, 100 milliseconds by default
    * @param f
    *   the function
    * @tparam T
    *   the type of what the function returns
    * @return
    *   a Try of the `f` function result
    */
  def retry[T](maxRetries: Int = 10, inBetweenSleep: FiniteDuration = 100.millis)(f: => T): Try[T] =
    retry(maxRetries, Option(inBetweenSleep))(f)
}
