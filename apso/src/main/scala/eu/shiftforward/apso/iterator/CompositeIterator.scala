package eu.shiftforward.apso.iterator

import scala.collection.GenTraversableOnce

/**
 * An iterator that wraps a list of other iterators and iterates over its
 * elements sequentially. It handles compositions of a large number of iterators
 * in a more efficient way than simply concatenating them, avoiding stack
 * overflows in particular. It supports appending of new iterators while keeping
 * its efficiency.
 * @param queue the list of iterators to compose
 * @tparam A the type of the elements to iterate over
 */
class CompositeIterator[A](
    private[iterator] var current: Iterator[A] = Iterator.empty,
    private[iterator] var iterators: IndexedSeq[Iterator[A]] = Vector()) extends Iterator[A] {

  def hasNext: Boolean = {
    if (!current.hasNext) {
      iterators = iterators.dropWhile { it =>
        current = it
        !current.hasNext
      }
      if (!iterators.isEmpty) iterators = iterators.drop(1)
    }

    current.hasNext
  }

  def next(): A =
    if (hasNext) current.next()
    else throw new NoSuchElementException("next on empty iterator")

  override def ++[B >: A](that: => GenTraversableOnce[B]): CompositeIterator[B] =
    CompositeIterator[B](this, that.toIterator)

}

/**
 * Companion object containing a factory for composite iterators.
 */
object CompositeIterator {
  def apply[A](its: Iterator[A]*): CompositeIterator[A] =
    its.headOption.map { head =>
      val zero =
        if (head.isInstanceOf[CompositeIterator[_]]) head.asInstanceOf[CompositeIterator[A]]
        else new CompositeIterator(iterators = Vector(head))

      its.tail.foldLeft(zero) {
        case (c, it) =>
          if (it.isInstanceOf[CompositeIterator[_]]) {
            val composite = it.asInstanceOf[CompositeIterator[A]]
            val iterators = composite.current +: composite.iterators

            new CompositeIterator(
              current = c.current,
              iterators = (c.iterators ++ iterators))

          } else {
            new CompositeIterator(current = c.current, iterators = (c.iterators :+ it))
          }
      }
    }.getOrElse(new CompositeIterator[A]())
}
