package eu.shiftforward.apso

import org.specs2.execute.{ AsResult, Result, Skipped }

trait JDKVersionHelper {
  def jdk[T](major: Int, minor: Int)(r: => T)(implicit evidence: AsResult[T]): Result = {
    val versionRegexp = """^([\d]+)\.([\d]+).*$""".r
    System.getProperty("java.version") match {
      case versionRegexp(ma, mi) if ma.toInt > major || ma.toInt == major && mi.toInt >= minor =>
        evidence.asResult(r)
      case _ =>
        Skipped("This test requires JDK >= %d.%d".format(major, minor))
    }
  }
}
