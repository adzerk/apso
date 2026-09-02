package com.kevel.apso

import java.util.concurrent.ThreadLocalRandom

import scala.annotation.tailrec
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, blocking}
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
        f.recoverWith {
          case NonFatal(
                _
              ) => // it would be indifferent to use a Throwable here because Futures don't catch Fatal exceptions
            inBetweenSleep.foreach(d => blocking(Thread.sleep(d.toMillis)))
            retryFuture[T](maxRetries - 1, inBetweenSleep)(f)
        }
    }
  }

  private[this] final def retry[T](maxRetries: Int, inBetweenSleep: Option[FiniteDuration])(f: => T): Try[T] = {
    maxRetries match {
      case 0 => Try(f)
      case _ =>
        Try(f) match {
          case res @ Success(_) => res
          case Failure(_)       =>
            inBetweenSleep.foreach(d => Thread.sleep(d.toMillis))
            retry[T](maxRetries - 1, inBetweenSleep)(f)
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

  /** Computes the duration to wait before the next attempt, growing exponentially with the number of attempts made.
    *
    * @param attempt
    *   the zero-based index of the attempt that failed
    * @param base
    *   the base waiting duration
    * @param max
    *   the optional duration with which to cap the exponential growth
    * @param factor
    *   the factor of the exponential duration
    * @param jitter
    *   the fraction of the waiting duration to randomize, from `0.0` for no jitter to `1.0` for a duration uniformly
    *   distributed between zero and the full waiting duration
    * @return
    *   the duration to wait before the next attempt
    */
  private[apso] def exponentialBackOffDelay(
      attempt: Int,
      base: FiniteDuration,
      max: Option[FiniteDuration] = None,
      factor: Double = 2.0,
      jitter: Double = 1.0
  ): FiniteDuration = {
    val exponential = (base.toMillis * Math.pow(factor, attempt.toDouble)).toLong
    val capped = max.fold(exponential)(m => Math.min(exponential, m.toMillis))
    val jitterFraction = Math.min(1.0, Math.max(0.0, jitter))
    (((1.0 - jitterFraction) * capped) + (ThreadLocalRandom
      .current()
      .nextDouble() * jitterFraction * capped)).toLong.millis
  }

  /** Performs a function `f` until it succeeds or until maximum retries is reached, waiting between attempts for a
    * duration that grows exponentially with the number of attempts made.
    *
    * @param maxRetries
    *   the number of retries
    * @param base
    *   the base waiting duration
    * @param max
    *   the optional duration with which to cap the exponential growth
    * @param factor
    *   the factor of the exponential duration
    * @param jitter
    *   the fraction of each waiting duration to randomize, from `0.0` for no jitter to `1.0` for a duration uniformly
    *   distributed between zero and the full waiting duration
    * @param retryWhen
    *   the predicate deciding whether a failure is worth retrying
    * @param onRetry
    *   the function called before each retry with the failure being retried, the duration that will be waited for and
    *   the number of retries still left
    * @param onMaxRetriesReached
    *   the function called when the max retries are reached with the latest failure
    * @param f
    *   the function to retry
    * @return
    *   a Try of the `f` function result
    */
  def exponentialBackOff[T](
      maxRetries: Int,
      base: FiniteDuration,
      max: Option[FiniteDuration] = None,
      factor: Double = 2.0,
      jitter: Double = 1.0,
      retryWhen: Throwable => Boolean = _ => true,
      onRetry: (Throwable, FiniteDuration, Int) => Unit = (_, _, _) => (),
      onMaxRetriesReached: Throwable => Unit = _ => ()
  )(f: => T): Try[T] = {
    @tailrec
    def aux(attempt: Int): Try[T] =
      Try(f) match {
        case res @ Success(_)      => res
        case failure @ Failure(ex) =>
          if (!retryWhen(ex)) failure
          else if (attempt >= maxRetries) {
            onMaxRetriesReached(ex)
            failure
          } else {
            val delay = exponentialBackOffDelay(attempt, base, max, factor, jitter)
            onRetry(ex, delay, maxRetries - attempt)
            Thread.sleep(delay.toMillis)
            aux(attempt + 1)
          }
      }

    aux(0)
  }
}
