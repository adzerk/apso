package eu.shiftforward.apso

import org.specs2.execute.{ AsResult, Result, Skipped }
import JreVersionTestHelper._
import scala.math.Ordering.Implicits._

trait JreVersionTestHelper {
  def jre[T](major: Int, minor: Int)(r: => T)(implicit evidence: AsResult[T]): Result = {
    System.getProperty("java.version") match {
      case VersionRegex(ma, mi) if (ma.toInt, mi.toInt) > (major, minor) => evidence.asResult(r)
      case _ => Skipped(s"This test requires JRE >= $major.$minor")
    }
  }
}

object JreVersionTestHelper {
  val VersionRegex = """^([\d]+)\.([\d]+).*$""".r
}
