package eu.shiftforward.apso.iterator

import scala.collection.mutable.ListBuffer

/**
 * A decorator for iterators adding more control over its lifetime.
 *
 * @param it the decorated iterator
 * @tparam A the type of the elements
 */
class ExtendedIterator[A](it: Iterator[A]) extends Iterator[A] {
  private[this] val onEndHook = ListBuffer.empty[() => Unit]

  def hasNext = {
    val n = it.hasNext
    if (!n) onEndHook.foreach(_.apply())
    n
  }

  def next() = it.next()

  def onEnd(block: => Unit): Unit = {
    onEndHook += { () => block }
  }
}
