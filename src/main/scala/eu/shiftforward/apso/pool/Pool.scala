package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

trait Pool[A] {
  protected def factory(): A
  protected def reset(a: A): A = a
  def acquire(): A
  def release(a: A)
}

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

object SimplePool {
  def apply[A](factory: => A, reset: A => A = identity[A] _) =
    new SimplePool(factory, reset)
}
