package com.velocidi.apso.caching.config

import scala.concurrent.duration.FiniteDuration

sealed trait Cache {
  def size: Option[Long]
  def ttl: Option[FiniteDuration]

  def implementation[V]: scalacache.Cache[V]
}

object Cache {
  case class Caffeine(size: Option[Long], ttl: Option[FiniteDuration]) extends Cache {
    def implementation[V]: scalacache.Cache[V] = {
      import scalacache._
      import scalacache.caffeine._

      val builder = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()

      CaffeineCache(
        size
          .map(builder.maximumSize)
          .getOrElse(builder)
          .build[String, Entry[V]]
      )
    }
  }

  case class Guava(size: Option[Long], ttl: Option[FiniteDuration]) extends Cache {
    def implementation[V]: scalacache.Cache[V] = {
      import com.google.common.cache.CacheBuilder
      import scalacache._
      import scalacache.guava._

      val builder = CacheBuilder.newBuilder()

      GuavaCache(
        size
          .map(builder.maximumSize)
          .getOrElse(builder)
          .build[String, Entry[V]]
      )
    }
  }
}
