package eu.shiftforward.apso.iterator

import scala.collection.GenTraversableOnce

/**
 * An iterator that wraps an array of other iterators and iterates over its
 * elements in a round-robin way.
 * @param iterators the array of iterators
 * @tparam A the type of the elements to iterate over
 */
class RoundRobinIterator[A](iterators: Array[() => Iterator[A]]) extends Iterator[A] {
  private[this] val its = iterators.map(_())
  private[this] val nIterators = its.length
  private[this] var current = 0

  def hasNext: Boolean = {
    if (its.length == 0)
      false
    else {
      var cycles = 0
      while (!its(current).hasNext && cycles < nIterators) {
        cycles += 1
        current = (current + 1) % nIterators
      }
      its(current).hasNext
    }
  }

  def next(): A = {
    val el =
      if (hasNext) its(current).next
      else throw new NoSuchElementException("empty iterator")
    current = (current + 1) % nIterators
    el
  }

  override def ++[B >: A](that: => GenTraversableOnce[B]): RoundRobinIterator[B] =
    new RoundRobinIterator[B](this.iterators ++ Array(() => that.toIterator))
}

/**
 * Companion object containing a factory for round-robin iterators.
 */
object RoundRobinIterator {
  def apply[A](its: () => Iterator[A]*): RoundRobinIterator[A] =
    new RoundRobinIterator[A](Array(its: _*))
}
