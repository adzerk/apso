package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

/**
 * A simple object pooling interface.
 * @tparam A the type of the objects to pool
 */
trait Pool[A] {
  /**
   * Factory for creating A objects.
   * @return a new A instance.
   */
  protected def factory(): A

  /**
   * Resets internal state of object.
   * @return the same instance received with internal state reset.
   */
  protected def reset(a: A): A = a

  /**
   * Obtains an instance from this pool
   * @return an instance from this pool.
   */
  def acquire(): A

  /**
   * Return an instance to the pool.
   */
  def release(a: A)
}

/**
 * A simplect object pool. It relies on Java synchronization for thread-safety.
 */
class SimplePool[A](_factory: => A, _reset: A => A) extends Pool[A] {
  private[this] val items = Queue[A]()

  protected def factory() = _factory
  override protected def reset(a: A) = _reset(a)

  def acquire(): A = synchronized {
    if (items.isEmpty) factory()
    else items.dequeue()
  }

  def release(a: A): Unit = synchronized {
    items.enqueue(reset(a))
  }
}

/**
 * Object containing factory methods for `SimplePool`.
 */
object SimplePool {
  def apply[A](factory: => A, reset: A => A = identity[A] _) =
    new SimplePool(factory, reset)
}
