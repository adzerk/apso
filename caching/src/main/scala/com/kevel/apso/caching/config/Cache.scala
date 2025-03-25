package com.kevel.apso.caching.config

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
}
