package eu.shiftforward.apso.pool

import scala.collection.mutable.Queue

trait KeyedPool[A, K] {
  protected def factory(k: K): A
  protected def reset(a: A): A = a
  protected def keyer(a: A, k: K): Boolean
  def acquire(key: K): A
  def release(a: A)
}

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

object SimpleKeyedPool {
  def apply[A, K](factory: K => A, keyer: (A, K) => Boolean, reset: A => A = identity[A] _) =
    new SimpleKeyedPool(factory, keyer, reset)
}
