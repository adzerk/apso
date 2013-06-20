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
class CompositeIterator[A](private[iterator] var queue: List[() => Iterator[A]]) extends Iterator[A] {
  private[this] var currentHead: Iterator[A] = null

  def hasNext: Boolean = {
    if (currentHead == null || !currentHead.hasNext) {
      queue = queue.dropWhile { it =>
        currentHead = it()
        !currentHead.hasNext
      }
      if (!queue.isEmpty) queue = queue.drop(1)
    }

    currentHead != null && currentHead.hasNext
  }

  def next(): A =
    if (hasNext) currentHead.next()
    else throw new NoSuchElementException("empty iterator")

  override def ++[B >: A](that: => GenTraversableOnce[B]): CompositeIterator[B] =
    new CompositeIterator[B](CompositeIterator.this.queue ++ List(() => that.toIterator))
}

/**
 * Companion object containing a factory for composite iterators.
 */
object CompositeIterator {
  def apply[A](its: () => Iterator[A]*): CompositeIterator[A] =
    new CompositeIterator[A](List(its: _*))

  def apply[A](its: => Seq[Iterator[A]]): Option[CompositeIterator[A]] =
    its.headOption.map { head =>
      val zero =
        if (head.isInstanceOf[CompositeIterator[_]]) head.asInstanceOf[CompositeIterator[A]]
        else CompositeIterator(() => head)

      its.tail.foldLeft(zero) {
        case (acc, it) =>
          if (it.isInstanceOf[CompositeIterator[_]]) {
            val composite = it.asInstanceOf[CompositeIterator[A]]
            CompositeIterator((acc.queue ++ composite.queue): _*)
          }
          else {
            acc ++ it
          }
      }
    }
}
