package eu.shiftforward.apso

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Utility object with retry mechanisms.
 */
object Retry {

  /**
   * Tries to perform a function `f` until it succeeds or until maximum retries is reached.
   *
   * @param maxRetries the number of retries, 10 by default
   * @param inBetweenSleep the milliseconds to wait between attempts, 100 milliseconds by default
   * @param f the function
   * @param ec the implicit execution context
   * @tparam T the type of what the future completes with
   * @return the resulting `f` function
   */
  @deprecated("This will be removed in a future version. Please use `retryFuture()` instead", "2017/10/31")
  def apply[T](
    maxRetries: Int = 10,
    inBetweenSleep: Option[Long] = Some(100))(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    retryFuture(maxRetries, inBetweenSleep.map(_.millis))(f)
  }

  /**
   * Tries to perform a Future[T] until it succeeds or until maximum retries is reached.
   *
   * @param maxRetries the number of retries, 10 by default
   * @param inBetweenSleep the duration to wait between attempts, 100 milliseconds by default
   * @param f the function that returns the Future[T]
   * @param ec the implicit execution context
   * @tparam T the type of what the future completes with
   * @return the Future[T]
   */
  def retryFuture[T](
    maxRetries: Int = 10,
    inBetweenSleep: Option[FiniteDuration] = Some(100.millis))(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    maxRetries match {
      case 0 =>
        f
      case _ =>
        f recoverWith {
          case _: Throwable =>
            inBetweenSleep.foreach(d => Thread.sleep(d.toMillis))
            retryFuture[T](maxRetries - 1, inBetweenSleep)(f)
        }
    }
  }

  /**
   * Tries to perform a function `f` until it succeeds or until maximum retries is reached.
   *
   * @param maxRetries the number of retries, 10 by default
   * @param inBetweenSleep the duration to wait between attempts, 100 milliseconds by default
   * @param f the function
   * @tparam T the type of what the function returns
   * @return a Try of the `f` function result
   */
  def retry[T](
    maxRetries: Int = 10,
    inBetweenSleep: Option[FiniteDuration] = Some(100.millis))(f: => T): Try[T] = {
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
}
