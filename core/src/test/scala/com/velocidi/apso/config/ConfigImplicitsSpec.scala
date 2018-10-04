package com.velocidi.apso.config

import scala.concurrent.duration._

import com.typesafe.config.{ Config, ConfigException, ConfigFactory }
import org.specs2.mutable.Specification

import com.velocidi.apso.config.Implicits._

@deprecated("Use https://github.com/pureconfig/pureconfig instead", "2017/07/13")
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
        n = 5s
        o = [1s, 5s]

        map {
          k1 = "v1"
          k2 = "v2"
        }

        num-map {
          k1 = 1
          k2 = 2
        }

        list-map {
          k1 = [{v = "v1"}, {v = "v2"}]
          k2 = [{v = "v3"}, {v = "v4"}]
        }

        list-map2 {
          k3 = ["v5", "v6"]
        }

        config {
          map {
            kx1 = "vx1"
          }
        }

        map-escape {
          a = "a"
          "(a,{} b. c)" = "abc"
          "x{}" = "abc"
          "_()#..!" = "a"
        }

        map-nested {
          a {
            b {
              "\ne" = "4"
              f = "7"
              "weird!$%" {
                "eml!hash" = 7
              }
            }
            c = "5"
          }
          d = "6"
        }

        "config.com" {
          "site.pt" = "ok"
        }

        config.com {
          site.pt = "ok2"
        }

      }""")

    "allow extracting configurations returning an option of a boolean" in {
      config.getBooleanOption("a") must beSome(true)
      config.getBooleanOption("a0") must beNone
      config.getBooleanOption("f") must throwAn[Exception]

      config.getOption[Boolean]("a") must beSome(true)
      config.getOption[Boolean]("a0") must beNone
      config.getOption[Boolean]("f") must throwAn[Exception]

    }

    "allow extracting configurations returning an option of a int" in {
      config.getIntOption("b") must beSome(2)
      config.getIntOption("b0") must beNone
      config.getIntOption("f") must throwAn[Exception]

      config.getOption[Int]("b") must beSome(2)
      config.getOption[Int]("b0") must beNone
      config.getOption[Int]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a long" in {
      config.getLongOption("c") must beSome(12345678901L)
      config.getLongOption("c0") must beNone
      config.getLongOption("f") must throwAn[Exception]

      config.getOption[Long]("c") must beSome(12345678901L)
      config.getOption[Long]("c0") must beNone
      config.getOption[Long]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a double" in {
      config.getDoubleOption("d") must beSome(3.3)
      config.getDoubleOption("d0") must beNone
      config.getDoubleOption("f") must throwAn[Exception]

      config.getOption[Double]("d") must beSome(3.3)
      config.getOption[Double]("d0") must beNone
      config.getOption[Double]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a string" in {
      config.getStringOption("e") must beSome("vdgf")
      config.getStringOption("e0") must beNone
      config.getStringOption("f") must throwAn[Exception]

      config.getOption[String]("e") must beSome("vdgf")
      config.getOption[String]("e0") must beNone
      config.getOption[String]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a config" in {
      config.getConfigOption("f") must beSome.which(_.getInt("f0") mustEqual 0)
      config.getConfigOption("f0") must beNone
      config.getConfigOption("e") must throwAn[Exception]
    }

    "get a percentage from a config file" in {
      config.getPercentage("b") mustEqual 2
      config.getPercentage("d") mustEqual 3.3
      config.getPercentage("g") mustEqual 0.302
      config.getPercentage("e") must throwAn[ConfigException.BadValue]
      config.getPercentage("h") must throwAn[ConfigException.BadValue]
      config.getPercentage("xkcd") must throwAn[ConfigException.Missing]
    }

    "allow extracting configurations returning an option of a percentage" in {
      config.getPercentageOption("g") must beSome(0.302)
      config.getPercentageOption("g0") must beNone
      config.getPercentageOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a duration" in {
      config.getDurationOption("n") must beSome(5.seconds)
      config.getDurationOption("n0") must beNone
      config.getDurationOption("e") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of boolean" in {
      config.getBooleanListOption("i") must beEqualTo(Some(List(true, false)))
      config.getBooleanListOption("i0") must beNone
      config.getBooleanListOption("f") must throwAn[Exception]

      config.getTypedListOption[Boolean]("i") must beEqualTo(Some(List(true, false)))
      config.getTypedListOption[Boolean]("i0") must beNone
      config.getTypedListOption[Boolean]("f") must throwAn[Exception]

      config.getOption[List[Boolean]]("i") must beEqualTo(Some(List(true, false)))
      config.getOption[List[Boolean]]("i0") must beNone
      config.getOption[List[Boolean]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of int" in {
      config.getTypedListOption("j") must beEqualTo(Some(List(1, 2)))
      config.getTypedListOption("j0") must beNone
      config.getTypedListOption("f") must throwAn[Exception]

      config.getTypedListOption[Int]("j") must beEqualTo(Some(List(1, 2)))
      config.getTypedListOption[Int]("j0") must beNone
      config.getTypedListOption[Int]("f") must throwAn[Exception]

      config.getOption[List[Int]]("j") must beEqualTo(Some(List(1, 2)))
      config.getOption[List[Int]]("j0") must beNone
      config.getOption[List[Int]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of long" in {
      config.getLongListOption("k") must beEqualTo(Some(List(12345678901L, 12345678902L)))
      config.getLongListOption("k0") must beNone
      config.getLongListOption("f") must throwAn[Exception]

      config.getTypedListOption[Long]("k") must beEqualTo(Some(List(12345678901L, 12345678902L)))
      config.getTypedListOption[Long]("k0") must beNone
      config.getTypedListOption[Long]("f") must throwAn[Exception]

      config.getOption[List[Long]]("k") must beEqualTo(Some(List(12345678901L, 12345678902L)))
      config.getOption[List[Long]]("k0") must beNone
      config.getOption[List[Long]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of double" in {
      config.getDoubleListOption("l") must beEqualTo(Some(List(1.1, 2.2)))
      config.getDoubleListOption("l0") must beNone
      config.getDoubleListOption("f") must throwAn[Exception]

      config.getTypedListOption[Double]("l") must beEqualTo(Some(List(1.1, 2.2)))
      config.getTypedListOption[Double]("l0") must beNone
      config.getTypedListOption[Double]("f") must throwAn[Exception]

      config.getOption[List[Double]]("l") must beEqualTo(Some(List(1.1, 2.2)))
      config.getOption[List[Double]]("l0") must beNone
      config.getOption[List[Double]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of string" in {
      config.getStringListOption("m") must beEqualTo(Some(List("a", "b")))
      config.getStringListOption("m0") must beNone
      config.getStringListOption("f") must throwAn[Exception]

      config.getTypedListOption[String]("m") must beEqualTo(Some(List("a", "b")))
      config.getTypedListOption[String]("m0") must beNone
      config.getTypedListOption[String]("f") must throwAn[Exception]

      config.getOption[List[String]]("m") must beEqualTo(Some(List("a", "b")))
      config.getOption[List[String]]("m0") must beNone
      config.getOption[List[String]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an list of durations" in {
      config.getDurationListOption("o") must beEqualTo(Some(List(1.second, 5.seconds)))
      config.getDurationListOption("o0") must beNone
      config.getDurationListOption("f") must throwAn[Exception]

      config.getOption[List[FiniteDuration]]("o") must beEqualTo(Some(List(1.second, 5.seconds)))
      config.getOption[List[FiniteDuration]]("o0") must beNone
      config.getOption[List[FiniteDuration]]("f") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a list of anything" in {
      config.getListOption("m") must beSome { x: List[Any] => x.size === 2 }
      config.getListOption("m0") must beNone
      config.getListOption("f") must throwAn[Exception]
    }

    "allow extracting configurations returning a map" in {
      config.getMap[String]("map") must beEqualTo(Map("k1" -> "v1", "k2" -> "v2"))
      config.getMap[Int]("num-map") must beEqualTo(Map("k1" -> 1, "k2" -> 2))
      config.getMap[String]("map-escape") must beEqualTo(
        Map("a" -> "a", "\"(a,{} b. c)\"" -> "abc", "\"x{}\"" -> "abc", "\"_()#..!\"" -> "a"))
      config.getFlattenedMap[String]("map-nested") must beEqualTo(
        Map("""a.b."\ne"""" -> "4", "a.b.f" -> "7", "a.c" -> "5", "d" -> "6", """a.b."weird!$%"."eml!hash"""" -> "7"))

      val confEscape = config.getConfig("map-escape")
      val mapEscape = confEscape.toMap[String]
      forall(mapEscape.keys) { k =>
        confEscape.hasPath(k) must not(throwA[ConfigException])
        confEscape.hasPath(k) must beTrue
      }
      mapEscape("a") must beEqualTo("a")
      mapEscape("\"(a,{} b. c)\"") must beEqualTo("abc")

      config.getConfig("map-nested").toMap[String] must throwA[ConfigException]
    }

    "allow extracting configurations returning an option of a map" in {
      config.getMapOption[String]("map") must beEqualTo(Some(Map("k1" -> "v1", "k2" -> "v2")))
      config.getMapOption[Int]("num-map") must beEqualTo(Some(Map("k1" -> 1, "k2" -> 2)))
      config.getConfig("num-map").toFlattenedMap[Int] must beEqualTo(Map("k1" -> 1, "k2" -> 2))
      config.getMapOption[String]("map0") must beNone
      config.getStringListOption("a") must throwAn[Exception]
    }

    "allow extracting configurations returning an option of a map of list of configs or something" in {
      config.getMapOption[List[Config]]("map0") must beNone

      val shallowMap = config.getMapOption[List[Config]]("list-map")
      val deepMap = config.getFlattenedMapOption[List[Config]]("list-map")
      shallowMap must beEqualTo(deepMap)
      shallowMap must beSome.which {
        map: Map[String, List[Config]] =>
          map.get("k1") must beSome.which { configs =>
            configs.map(_.getString("v")) must beEqualTo(List("v1", "v2"))
          }
          map.get("k2") must beSome.which { configs =>
            configs.map(_.getString("v")) must beEqualTo(List("v3", "v4"))
          }
      }

      config.getMapOption[List[String]]("list-map2") must beSome.which {
        _.get("k3") must beSome.which(_ === List("v5", "v6"))
      }
    }

    "allow extracting configurations returning a map of configs" in {
      val map = config.toConfigMap
      map("map").getString("k1") === "v1"
      map("map").getString("k2") === "v2"
      map("\"config.com\"").getString("\"site.pt\"") === "ok"
      map("config").getString("com.site.pt") === "ok2"

      config.getConfigMapOption("map0") must beNone
      config.getConfigMapOption("config") must beSome.which { _("map").getString("kx1") === "vx1" }
    }

    "allow converting configurations into a map with keys that are valid paths" in {
      val confNested = config.getConfig("map-nested")
      forall(confNested.toFlattenedMap[Unit].keys) { k =>
        confNested.hasPath(k) must not(throwA[ConfigException])
        confNested.hasPath(k) must beTrue
      }
      val confEscape = config.getConfig("map-escape")
      forall(confEscape.toFlattenedMap[String].keys) { k =>
        confEscape.hasPath(k) must not(throwA[ConfigException])
        confEscape.hasPath(k) must beTrue
      }
      forall(confEscape.toMap[String].keys) { k =>
        confEscape.hasPath(k) must not(throwA[ConfigException])
        confEscape.hasPath(k) must beTrue
      }
    }
  }
}
