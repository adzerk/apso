package com.velocidi.apso

import org.apache.logging.log4j.scala.Logger

/**
 * Trait to mixin a Log4j2 `Logger` object. The `Logger` object is initialized lazily.
 */
trait Logging {
  /**
   * The `Logger` object. This logger will have the same name as the concrete class
   * into which this trait is mixed-in.
   */
  lazy val log = Logger(getClass)
}

/**
 * Trait to mixin a Log4j2 `Logger` object. The `Logger` object is initialized strictly.
 */
trait StrictLogging {
  /**
   * The `Logger` object. This logger will have the same name as the concrete class
   * into which this trait is mixed-in.
   */
  val log = Logger(getClass)
}
