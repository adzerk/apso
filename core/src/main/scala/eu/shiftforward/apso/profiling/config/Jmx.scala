package eu.shiftforward.apso.profiling.config

/**
 * Config class for SimpleJmx
 *
 * @param host the optional host
 * @param port the optional port
 */
case class Jmx(host: Option[String], port: Option[Int])
