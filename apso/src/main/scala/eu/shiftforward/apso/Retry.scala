package eu.shiftforward.apso

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Utility object for retrying Future a number of times.
 */
object Retry {

  /**
   * Tries to perform a function `f` until it succeeds or until maximum retries is reached.
   *
   * @param maxRetries the number of retries, 10 by default
   * @param inBetweenSleep the milliseconds to wait between attempts, 100 milliseconds by default
   * @param f the function
   * @tparam T the type of that the future completes with
   * @return the resulting `f` function
   */
  def apply[T](maxRetries: Int = 10,
               inBetweenSleep: Option[Long] = Some(100))(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    maxRetries match {
      case 0 =>
        f
      case _ =>
        f recoverWith {
          case _ =>
            inBetweenSleep.foreach(Thread.sleep)
            apply(maxRetries - 1, inBetweenSleep)(f)
        }
    }
  }
}
