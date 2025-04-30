package com.kevel.apso.caching.config

import scala.concurrent.duration.FiniteDuration

import com.github.blemale.scaffeine
import com.github.blemale.scaffeine.Scaffeine

/** A cache configuration for the underlying [[https://github.com/ben-manes/caffeine Caffeine]] cache implementation.
  *
  * @param timeToLive
  *   the maximum time an entry can stay in cache after its last write. If empty, the entry will not be evicted based on
  *   age.
  * @param maximumSize
  *   the maximum number of entries in the cache. Note that this threshold might be temporarily exceeded while the
  *   underlying cache is evicting old entries.
  */
case class Cache(timeToLive: Option[FiniteDuration], maximumSize: Option[Long]) {
  private[caching] def build: Scaffeine[Any, Any] = {
    val withSize = maximumSize.fold(Scaffeine())(Scaffeine().maximumSize)
    val withTimeToLive = timeToLive.fold(withSize)(withSize.expireAfterWrite)
    withTimeToLive
  }

  /** A [[com.github.blemale.scaffeine.Scaffeine]] cache implementation for this configuration. Use this over the
    * [[com.kevel.apso.caching.CachedFunctionsExtras]] if you need finer-grained control over cache insertions.
    *
    * @tparam K
    *   the key type.
    * @tparam V
    *   the value type.
    * @return
    *   a [[com.github.blemale.scaffeine.Scaffeine]] cache implementation.
    */
  def implementation[K, V]: scaffeine.Cache[K, V] = build.build()
}
