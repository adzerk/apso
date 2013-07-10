package eu.shiftforward.apso

import com.github.nscala_time.time.Imports._

/**
 * Trait representing objects with logging abilities.
 */
@deprecated("Use the `Logging` trait instead", "0.1")
trait Loggable {

  /**
   * Debug log level.
   */
  final val DEBUG = 0

  /**
   * Info log level.
   */
  final val INFO = 1

  /**
   * Warn log level.
   */
  final val WARN = 2

  /**
   * Error log level.
   */
  final val ERROR = 3

  val minPriority = DEBUG

  private lazy val logger = new {
    val isDebugEnabled = minPriority <= DEBUG
    val isInfoEnabled = minPriority <= INFO
    val isWarnEnabled = minPriority <= WARN
    val isErrorEnabled = minPriority <= ERROR

    def debug(msg: AnyRef, t: => Throwable = null) { println(s"[DEBUG] [${DateTime.now}] " + msg) }
    def info(msg: AnyRef, t: => Throwable = null) { println(s"[INFO] [${DateTime.now}] " + msg) }
    def warn(msg: AnyRef, t: => Throwable = null) { println(s"[WARN] [${DateTime.now}] " + msg) }
    def error(msg: AnyRef, t: => Throwable = null) { println(s"[ERROR] [${DateTime.now}] " + msg) }
  }

  /**
   * Logs a message with DEBUG level.
   * @param msg the message to log
   * @param t an optional throwable to associate with the message
   */
  protected def debug(msg: => AnyRef, t: => Throwable = null) {
    if (logger.isDebugEnabled) {
      if (t != null) {
        logger.debug(msg.toString, t)
      }
      else {
        logger.debug(msg.toString)
      }
    }
  }

  /**
   * Logs a message with INFO level.
   * @param msg the message to log
   * @param t an optional throwable to associate with the message
   */
  protected def info(msg: => AnyRef, t: => Throwable = null) {
    if (logger.isInfoEnabled) {
      if (t != null) {
        logger.info(msg.toString, t)
      }
      else {
        logger.info(msg.toString)
      }
    }
  }

  /**
   * Logs a message with ERROR level.
   * @param msg the message to log
   * @param t an optional throwable to associate with the message
   */
  protected def error(msg: => AnyRef, t: => Throwable = null) {
    if (logger.isErrorEnabled) {
      if (t != null) {
        logger.error(msg.toString, t)
      }
      else {
        logger.error(msg.toString)
      }
    }
  }

  /**
   * Logs a message with WARN level.
   * @param msg the message to log
   * @param t an optional throwable to associate with the message
   */
  protected def warn(msg: => AnyRef, t: => Throwable = null) {
    if (logger.isWarnEnabled) {
      if (t != null) {
        logger.warn(msg.toString, t)
      }
      else {
        logger.warn(msg.toString)
      }
    }
  }
}
