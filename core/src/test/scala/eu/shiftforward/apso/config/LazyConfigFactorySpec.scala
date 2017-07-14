package eu.shiftforward.apso.config

import com.typesafe.config.{ ConfigException, ConfigFactory }
import org.specs2.mutable.Specification

@deprecated("Some of methods tested here will be removed later", "2017/07/13")
class LazyConfigFactorySpec extends Specification {
  sequential

  "A LazyConfigLoader" should {

    "Resolve variables in reference.conf only when application.conf is included" in {
      ConfigFactory.load.getString("host2") mustEqual "localhost"
      LazyConfigFactory.load.getString("host2") mustEqual "192.168.59.103"
    }

    "Resolve variables in reference.conf only when a custom config is included" in {
      ConfigFactory.load("test").getString("host2") mustEqual "localhost"
      LazyConfigFactory.load("test").getString("host2") mustEqual "192.168.59.104"
    }

    "Load an overrides.conf that has priority over application.conf if it exists" in {
      ConfigFactory.load.getString("path2") mustEqual "."
      LazyConfigFactory.load.getString("path2") mustEqual System.getenv.get("PATH")
    }

    "Have a config loading DSL" in {

      "Load a config in a given key" in {
        LazyConfigFactory.loadAt("a").config.getInt("a1") mustEqual 2
        LazyConfigFactory.loadAt("a.b").config.getInt("b1") mustEqual 5
      }

      "Load a config in a given key with some overrides" in {
        LazyConfigFactory.loadAt("a.b").withOverrides("b1" -> 6).config.getInt("b1") mustEqual 6
        LazyConfigFactory.loadAt("a.b").withOverrides("b2" -> 6).config must throwA[ConfigException.ValidationFailed]
      }

      "Load a config in a given key with some extra settings" in {
        LazyConfigFactory.loadAt("a.b").withSettings("b2" -> "new").config.getString("b2") mustEqual "new"
        LazyConfigFactory.loadAt("a.b").withSettings("b1" -> "new").config must throwA[ConfigException.ValidationFailed]
      }

      "Support different ways to provide configs in withSettings and withOverrides" in {
        LazyConfigFactory.loadAt("a").withOverrides(ConfigFactory.parseString("{ a1 = 2, a2 = aa }"))
          .config.getString("a2") mustEqual "aa"

        LazyConfigFactory.loadAt("a").withOverrides("{ a1 = 2, a2 = aa }")
          .config.getString("a2") mustEqual "aa"

        LazyConfigFactory.loadAt("a").withOverrides("a1" -> 2, "a2" -> "aa")
          .config.getString("a2") mustEqual "aa"

        LazyConfigFactory.loadAt("a").withSettings(ConfigFactory.parseString("{ a3 = 2, a4 = aa }"))
          .config.getInt("a3") mustEqual 2

        LazyConfigFactory.loadAt("a").withSettings("{ a3 = 2, a4 = aa }")
          .config.getInt("a3") mustEqual 2

        LazyConfigFactory.loadAt("a").withSettings("a3" -> 2, "a4" -> "aa")
          .config.getInt("a3") mustEqual 2
      }
    }
  }
}
