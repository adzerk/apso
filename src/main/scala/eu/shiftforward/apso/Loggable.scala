package eu.shiftforward.apso

import com.github.nscala_time.time.Imports._

trait Loggable {
  final val DEBUG = 0
  final val INFO = 1
  final val WARN = 2
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
