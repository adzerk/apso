package eu.shiftforward.apso

import scala.collection.mutable.HashMap

/**
 * Base trait for memoizing a function. In the base caching strategy, the cache grows indefinitely, so use with caution. See [[eu.shiftforward.apso.Memo]] and [[eu.shiftforward.apso.Memo2]].
 *
 * @tparam In The (possibly product) type of the function that is memoized.
 * @tparam Out The type of the result.
 */
trait Memoization[-In, +Out] {
  protected[this] val cache = HashMap.empty[In, Out]

  /**
   * Un-memoizes all previously memoized results.
   */
  def clear() { cache.clear() }
}

/**
 * Trait that mixes in functions useful for directly manipulating the caching mechanisms used by [[eu.shiftforward.apso.Memoization]]
 *
 * @tparam In The (possibly product) type of the function that is memoized.
 * @tparam Out The type of the result.
 */
trait MemoizationStats[-In, +Out] extends Memoization[In, Out] {
  private[this] var cacheHit: Int = 0
  private[this] var cacheMiss: Int = 0

  protected[this] override val cache = new HashMap[In, Out] {
    override def getOrElseUpdate(key: In, op: => Out): Out =
      get(key) match {
        case Some(v) =>
          cacheHit += 1; v
        case None => val d = op; this(key) = d; cacheMiss += 1; d
      }
  }

  /**
   * Returns how many results are currently memoized.
   */
  def size = cache.size

  /**
   * Returns the number of times a memoized result was used instead of invoking the function.
   */
  def hits = cacheHit

  /**
   * Returns the number of times a new result was calculated.
   */
  def misses = cacheMiss
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
 * @param f The function to be memoized.
 * @tparam A The type on the first parameter of the function to be memoized.
 * @tparam B The return type of the function to be memoized.
 */
case class Memo[-A, +B](f: A => B) extends (A => B) with Memoization[A, B] {
  def apply(x: A) = cache getOrElseUpdate (x, f(x))
}

/**
 * Generic case class for memoizing a function of arity 2. See for more details. [[eu.shiftforward.apso.Memo]]
 *
 * @param f The function to be memoized.
 * @tparam A The type on the first parameter of the function to be memoized.
 * @tparam B The type on the second parameter of the function to be memoized.
 * @tparam C The return type of the function to be memoized.
 */
case class Memo2[-A, -B, +C](f: (A, B) => C) extends ((A, B) => C) with Memoization[(A, B), C] {
  def apply(x: A, y: B) = cache getOrElseUpdate ((x, y), f(x, y))
}