package eu.shiftforward.apso

import scala.collection.mutable.HashMap

/**
 * Base trait for memoizing a function. See [[eu.shiftforward.apso.Memo]] and [[eu.shiftforward.apso.Memo2]].
 */
trait Memoization[-In, +Out] {
  protected[this] val cache = HashMap.empty[In, Out]
}

/**
 * Trait that mixes in functions useful for directly manipulating the caching mechanisms used by [[eu.shiftforward.apso.Memoization]]
 */

trait MemoizationStats[-In, +Out] extends Memoization[In, Out] {
  /**
   * Returns the size of the cache.
   */
  def size = cache.size

  /**
   * Clears all objects that were memoized.
   */
  def clear() = cache.clear()
}

/**
 * Generic case class for memoizing a function with only one parameter. The cache grows indefinitely, so use with caution. Example:
 *
 * {{{
 * val complex = Memo { x: Int = x + 2 }
 * complex(2)  // Parameter was never seen. Result is calculated and stored in cache.
 * complex(3)  // Parameter was never seen. Result is calculated and stored in cache.
 * complex(2)  // Parameter was seen. Result is returned from cache
 * }}}
 *
 */
case class Memo[A, B](f: A => B) extends (A => B) with Memoization[A, B] {
  def apply(x: A) = cache getOrElseUpdate (x, f(x))
}

/**
 * Generic case class for memoizing a function with two parameters. The cache grows indefinitely, so use with caution. See [[eu.shiftforward.apso.Memo]]
 *
 */
case class Memo2[A, B, C](f: (A, B) => C) extends ((A, B) => C) with Memoization[(A, B), C] {
  def apply(x: A, y: B) = cache getOrElseUpdate ((x, y), f(x, y))
}