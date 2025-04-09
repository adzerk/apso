package com.kevel.apso.caching.config

import scala.concurrent.duration.FiniteDuration

/** A cache configuration for the underlying [[https://github.com/ben-manes/caffeine Caffeine]] cache implementation.
  *
  * @param timeToLive
  *   the maximum time an entry can stay in cache after its last write. If empty, the entry will not be evicted based on
  *   age.
  * @param maximumSize
  *   the maximum number of entries in the cache. Note that this threshold might be temporarily exceeded while the
  *   underlying cache is evicting old entries.
  */
case class Cache(timeToLive: Option[FiniteDuration], maximumSize: Option[Long] = None)
