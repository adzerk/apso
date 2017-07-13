package eu.shiftforward.apso

import org.slf4j.LoggerFactory

/**
 * Trait to mixin a slf4j `Logger` object. The `Logger` object is initialized lazily.
 */
trait Logging {
  /**
   * The `Logger` object. This logger will have the same name as the concrete class
   * into which this trait is mixed-in.
   */
  lazy val log = LoggerFactory.getLogger(getClass.getName)
}

/**
 * Trait to mixin a slf4j `Logger` object. The `Logger` object is initialized strictly.
 */
trait StrictLogging {
  /**
   * The `Logger` object. This logger will have the same name as the concrete class
   * into which this trait is mixed-in.
   */
  val log = LoggerFactory.getLogger(getClass.getName)
}
