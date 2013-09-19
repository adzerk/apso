package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

/**
 * A simple object pool where all objects instances are "equal",
 * i.e. have no distincting "specs".
 * It relies on Java synchronization for thread-safety.
 */
class UnrestrictedPool[A](_factory: => A, _reset: A => A) extends Pool[A, Unit] {
  private[this] val items = Queue[A]()

  protected def factory(u: Unit) = _factory
  override protected def reset(a: A) = _reset(a)

  protected def obeys(a: A, s: Unit) = true

  def acquire(u: Unit): A = synchronized {
    if (items.isEmpty) factory()
    else items.dequeue()
  }

  def release(a: A): Unit = synchronized {
    items.enqueue(reset(a))
  }
}

/**
 * Object containing factory methods for `UnrestrictedPool`.
 */
object UnrestrictedPool {
  def apply[A](factory: => A, reset: A => A = identity[A] _) =
    new UnrestrictedPool(factory, reset)
}
