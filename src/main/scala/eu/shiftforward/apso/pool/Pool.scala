package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

/**
 * An object pooling interface.
 * A pool pools instances with different "specs" (e.g. array size).
 * Each instance may be accessed using an arbitrary spec.
 * @tparam A the type of the objects to pool
 * @tparam S the type of specs for object in the pool
 */
trait Pool[A, S] {
  /**
   * Factory for creating A objects with spec B.
   * @return a new A instance.
   */
  protected def factory(s: S): A

  /**
   * Resets internal state of object.
   * @return the same instance received with internal state reset.
   */
  protected def reset(a: A): A = a

  /**
   * Returns whether or not an object obeys a given spec.
   * @param a The object to test
   * @param spec The spec to test
   * @return true if, and only if, this object obeys spec `s`.
   */
  protected def obeys(a: A, s: S): Boolean

  /**
   * Obtains an instance from this pool given the specified spec.
   * @return an instance from this pool.
   */
  def acquire(s: S): A

  /**
   * Return an instance to the pool.
   */
  def release(a: A)
}

/**
 * A simple object pool. It relies on Java synchronization for thread-safety.
 */
class SimplePool[A, S](_factory: S => A, _obeys: (A, S) => Boolean, _reset: A => A) extends Pool[A, S] {
  private[this] val items = Queue[A]()

  protected def factory(s: S) = _factory(s)
  override protected def reset(a: A) = _reset(a)
  protected def obeys(a: A, s: S) = _obeys(a, s)

  def acquire(s: S): A = synchronized {
    if (items.isEmpty) factory(s)
    else {
      val a = items.dequeue()
      if (!obeys(a, s)) factory(s)
      else a
    }
  }

  def release(a: A): Unit = synchronized {
    items.enqueue(reset(a))
  }
}

/**
 * Object containing factory methods for `SimplePool`.
 */
object SimplePool {
  def apply[A, S](factory: S => A, obeys: (A, S) => Boolean, reset: A => A = identity[A] _) =
    new SimplePool(factory, obeys, reset)
}
