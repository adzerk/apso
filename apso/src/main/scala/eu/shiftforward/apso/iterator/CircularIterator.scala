package eu.shiftforward.apso.iterator

/**
 * A wrapper around an iterator that iterates over its elements in a circular
 * way.
 * @param it the inner iterator
 * @tparam A the type of the elements to iterate over
 */
class CircularIterator[A](it: => Iterator[A]) extends Iterator[A] {
  var currentIt = it

  def next() = {
    if (!currentIt.hasNext)
      currentIt = it
    currentIt.next()
  }

  def hasNext = true
}

/**
 * Companion object containing a factory for circular iterators.
 */
object CircularIterator {
  def apply[A](it: => Iterator[A]) =
    new CircularIterator(it)
}
