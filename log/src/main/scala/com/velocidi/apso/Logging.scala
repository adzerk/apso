package com.velocidi.apso

import com.typesafe.scalalogging.Logger

/** Trait to mixin an SLF4J `Logger` object. The `Logger` object is initialized lazily.
  */
trait Logging {

  /** The `Logger` object. This logger will have the same name as the concrete class into which this trait is mixed-in.
    */
  lazy val log = Logger(getClass)
}

/** Trait to mixin an SLF4J `Logger` object. The `Logger` object is initialized strictly.
  */
trait StrictLogging {

  /** The `Logger` object. This logger will have the same name as the concrete class into which this trait is mixed-in.
    */
  val log = Logger(getClass)
}
