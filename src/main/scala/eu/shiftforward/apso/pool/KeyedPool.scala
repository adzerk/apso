package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

/**
 * A "keyed" object pooling interface.
 * A keyed pool pools instances of multiple "types".
 * Each "type" may be accessed using an arbitrary key.
 * @tparam A the type of the objects to pool
 * @tparam K the type of keys for object in the pool
 */
trait KeyedPool[A, K] {
  /**
   * Factory for creating A objects with key K.
   * @return a new A instance.
   */
  protected def factory(k: K): A

  /**
   * Resets internal state of object.
   * @return the same instance received with internal state reset.
   */
  protected def reset(a: A): A = a

  /**
   * Returns wheter or not an object has a certain key.
   * @param a The object to test
   * @param k The key to test
   * @return true if, and only if, this object has key `k`.
   */
  protected def keyer(a: A, k: K): Boolean

  /**
   * Obtains an instance from this pool for the specified key.
   * @return an instance from this pool.
   */
  def acquire(key: K): A

  /**
   * Return an instance to the pool.
   */
  def release(a: A)
}

/**
 * A simplect "keyed" object pool. It relies on Java synchronization for thread-safety.
 */
class SimpleKeyedPool[A, K](_factory: K => A, _keyer: (A, K) => Boolean, _reset: A => A) extends KeyedPool[A, K] {
  private[this] val items = Queue[A]()

  protected def factory(k: K) = _factory(k)
  override protected def reset(a: A) = _reset(a)
  protected def keyer(a: A, k: K) = _keyer(a, k)

  def acquire(k: K): A = synchronized {
    if (items.isEmpty) factory(k)
    else {
      val a = items.dequeue()
      if (!keyer(a, k)) factory(k)
      else a
    }
  }

  def release(a: A): Unit = synchronized {
    items.enqueue(reset(a))
  }
}

/**
 * Object containing factory methods for `SimpleKeyedPool`.
 */
object SimpleKeyedPool {
  def apply[A, K](factory: K => A, keyer: (A, K) => Boolean, reset: A => A = identity[A] _) =
    new SimpleKeyedPool(factory, keyer, reset)
}
