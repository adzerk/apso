package com.velocidi.apso.iterator

import scala.collection.{AbstractIterator, BufferedIterator, Iterator}

/** An iterator that wraps a list of other iterators and iterates over its elements sequentially. It handles
  * compositions of a large number of iterators in a more efficient way than simply concatenating them, avoiding stack
  * overflows in particular. It supports appending of new iterators while keeping its efficiency.
  * @param iterators
  *   the list of iterators to compose
  * @tparam A
  *   the type of the elements to iterate over
  */
@deprecated("The stack overflow caused by Iterator.++ should be fixed in recent Scala versions.", "0.15.0")
class CompositeIterator[A](
    private[iterator] var current: Iterator[A] = Iterator.empty,
    private[iterator] var iterators: IndexedSeq[Iterator[A]] = Vector()
) extends Iterator[A] { self =>

  def hasNext: Boolean = {
    if (!current.hasNext) {
      iterators = iterators.dropWhile { it =>
        current = it
        !current.hasNext
      }
      if (iterators.nonEmpty) iterators = iterators.drop(1)
    }

    current.hasNext
  }

  def next(): A =
    if (hasNext) current.next()
    else throw new NoSuchElementException("next on empty iterator")

  // This forces the head to be stored only on the current iterator
  override def buffered: BufferedIterator[A] = {
    current = current.buffered
    iterators = iterators.map(_.buffered)
    new AbstractIterator[A] with BufferedIterator[A] { buff =>
      def head: A =
        if (hasNext) current.asInstanceOf[BufferedIterator[A]].head
        else throw new NoSuchElementException("next on empty iterator")
      def next(): A = self.next()
      def hasNext: Boolean = self.hasNext

      // Methods that implement their own buffering strategy, therefore need to be overriden
      override def filter(p: (A) => Boolean): Iterator[A] =
        new CompositeIterator[A](current.filter(p), iterators.map(_.filter(p)))

      override def takeWhile(p: A => Boolean): Iterator[A] = new AbstractIterator[A] {
        def hasNext: Boolean = buff.hasNext && p(buff.head)
        def next(): A =
          if (hasNext) buff.next()
          else throw new NoSuchElementException("next on empty iterator")
      }

      override def dropWhile(p: A => Boolean): Iterator[A] = new AbstractIterator[A] {
        def hasNext: Boolean = {
          current = current.dropWhile(p)
          if (!current.hasNext) {
            iterators = iterators.dropWhile { it =>
              current = it.dropWhile(p)
              !current.hasNext
            }
            if (iterators.nonEmpty) iterators = iterators.drop(1)
          }
          current.hasNext
        }
        def next(): A =
          if (hasNext) current.next()
          else throw new NoSuchElementException("next on empty iterator")
      }

    }
  }
}

/** Companion object containing a factory for composite iterators.
  */
object CompositeIterator {
  def apply[A](its: Iterator[A]*): CompositeIterator[A] =
    its.headOption
      .map { head =>
        val zero =
          if (head.isInstanceOf[CompositeIterator[_]]) head.asInstanceOf[CompositeIterator[A]]
          else new CompositeIterator(iterators = Vector(head))

        its.tail.foldLeft(zero) { case (c, it) =>
          if (it.isInstanceOf[CompositeIterator[_]]) {
            val composite = it.asInstanceOf[CompositeIterator[A]]
            val iterators = composite.current +: composite.iterators

            new CompositeIterator(current = c.current, iterators = c.iterators ++ iterators)

          } else {
            new CompositeIterator(current = c.current, iterators = c.iterators :+ it)
          }
        }
      }
      .getOrElse(new CompositeIterator[A]())
}
