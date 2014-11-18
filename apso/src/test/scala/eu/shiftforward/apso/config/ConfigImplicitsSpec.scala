package eu.shiftforward.apso.config

import com.typesafe.config.{ ConfigException, ConfigFactory }
import eu.shiftforward.apso.Implicits._
import org.specs2.mutable.Specification

class ConfigImplicitsSpec extends Specification {

  "An ApsoConfig" should {
    val config = ConfigFactory.parseString(
      """ {
        a = true
        b = 2
        c = 12345678901
        d = 3.3
        e = vdgf
        f = { f0 = 0 }
        h = 30.2%
      }""")

    "get a percentage from a config file" in {
      config.getPercentage("h") mustEqual 0.302
      config.getPercentage("b") must throwAn[ConfigException.BadValue]
      config.getPercentage("xkcd") must throwAn[ConfigException.Missing]
    }

    "allow extracting configurations returning an option when they are not present" in {
      config.getBooleanOption("a") must beSome(true)
      config.getBooleanOption("a0") must beNone
      config.getBooleanOption("f") must throwAn[Exception]

      config.getIntOption("b") must beSome(2)
      config.getIntOption("b0") must beNone
      config.getIntOption("f") must throwAn[Exception]

      config.getLongOption("c") must beSome(12345678901L)
      config.getLongOption("c0") must beNone
      config.getLongOption("f") must throwAn[Exception]

      config.getDoubleOption("d") must beSome(3.3)
      config.getDoubleOption("d0") must beNone
      config.getDoubleOption("f") must throwAn[Exception]

      config.getStringOption("e") must beSome("vdgf")
      config.getStringOption("e0") must beNone
      config.getStringOption("f") must throwAn[Exception]

      config.getPercentageOption("h") must beSome(0.302)
      config.getPercentageOption("h0") must beNone
      config.getPercentageOption("f") must throwAn[Exception]
    }
  }
}
