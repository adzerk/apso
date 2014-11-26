package eu.shiftforward.apso.config

import com.typesafe.config.{ ConfigException, ConfigFactory }
import eu.shiftforward.apso.config.Implicits._
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
        g = 30.2%
        h = soclose%
        i = [true, false]
        j = [1, 2]
        k = [12345678901, 12345678902]
        l = [1.1, 2.2]
        m = ["a", "b"]
      }""")

    "get a percentage from a config file" in {
      config.getPercentage("b") mustEqual 2
      config.getPercentage("d") mustEqual 3.3
      config.getPercentage("g") mustEqual 0.302
      config.getPercentage("e") must throwAn[ConfigException.BadValue]
      config.getPercentage("h") must throwAn[ConfigException.BadValue]
      config.getPercentage("xkcd") must throwAn[ConfigException.Missing]
    }

    "allow extracting configurations returning an option of a boolean" in {
      config.getBooleanOption("a") must beSome(true)
      config.getBooleanOption("a0") must beNone
      config.getBooleanOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a int" in {
      config.getIntOption("b") must beSome(2)
      config.getIntOption("b0") must beNone
      config.getIntOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a long" in {
      config.getLongOption("c") must beSome(12345678901L)
      config.getLongOption("c0") must beNone
      config.getLongOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a double" in {
      config.getDoubleOption("d") must beSome(3.3)
      config.getDoubleOption("d0") must beNone
      config.getDoubleOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a string" in {
      config.getStringOption("e") must beSome("vdgf")
      config.getStringOption("e0") must beNone
      config.getStringOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a percentage" in {
      config.getPercentageOption("g") must beSome(0.302)
      config.getPercentageOption("g0") must beNone
      config.getPercentageOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of boolean" in {
      config.getBooleanListOption("i") must beEqualTo(Some(List(true, false)))
      config.getBooleanListOption("i0") must beNone
      config.getBooleanListOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of int" in {
      config.getIntListOption("j") must beSome(List(1, 2))
      config.getIntListOption("j0") must beNone
      config.getIntListOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of long" in {
      config.getLongListOption("k") must beSome(List(12345678901L, 12345678902L))
      config.getLongListOption("k0") must beNone
      config.getLongListOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of double" in {
      config.getDoubleListOption("l") must beSome(List(1.1, 2.2))
      config.getDoubleListOption("l0") must beNone
      config.getDoubleListOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of string" in {
      config.getStringListOption("m") must beSome(List("a", "b"))
      config.getStringListOption("m0") must beNone
      config.getStringListOption("f") must throwAn[Exception]
    }

  }
}
