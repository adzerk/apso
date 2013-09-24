package eu.shiftforward.apso

import org.slf4j.LoggerFactory

/**
 * Trait to mixin a slf4j `Logger` object.
 */
trait Logging {
  /**
   * The `Logger` object. This logger will have the same name as the concrete class
   * into which this trait is mixed-in.
   */
  lazy val log = LoggerFactory.getLogger(this.getClass.getName)
}
