package eu.shiftforward.apso

import org.specs2.matcher.ResultMatchers
import org.specs2.mutable.Specification

class JreVersionHelperSpec extends Specification with ResultMatchers {

  "A JreVersionHelper" should {
    "Return a parsed JRE version" in {
      val (major, minor) = JreVersionHelper.jreVersion
      System.getProperty("java.version") must beMatching(s"$major\\.$minor.*")
    }
  }
}
