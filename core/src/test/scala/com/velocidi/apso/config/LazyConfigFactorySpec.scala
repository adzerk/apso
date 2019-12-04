package com.velocidi.apso.config

import org.specs2.mutable.Specification

class LazyConfigFactorySpec extends Specification {
  sequential

  "A LazyConfigLoader" should {

    "Resolve variables in reference.conf only when application.conf is included" in {
      LazyConfigFactory.load.getString("host2") mustEqual "192.168.59.103"
    }

    "Resolve variables in reference.conf only when a custom config is included" in {
      LazyConfigFactory.load("test").getString("host2") mustEqual "192.168.59.104"
    }

    "Load an overrides.conf that has priority over application.conf if it exists" in {
      LazyConfigFactory.load.getString("path2") mustEqual System.getenv.get("PATH")
    }
  }
}
