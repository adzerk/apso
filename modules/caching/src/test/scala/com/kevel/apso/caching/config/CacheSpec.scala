package com.kevel.apso.caching.config

import scala.concurrent.duration.DurationInt

import org.specs2.mutable.Specification

class CacheSpec extends Specification {
  "A Cache configuration instance" should {
    "provide an `implementation` method that exposes a configured `Scaffeine` implementation" in {
      val cache = Cache(Some(500.millis), None).implementation[String, String]
      cache.getIfPresent("john") === None
      cache.put("john", "doe")
      cache.getIfPresent("john") === Some("doe")
      eventually(10, 100.millis)(cache.getIfPresent("john") === None)
      cache.put("john", "rivers")
      cache.getIfPresent("john") === Some("rivers")
    }
  }
}
