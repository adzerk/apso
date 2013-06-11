package eu.shiftforward.apso.iterator

class CircularIterator[A](it: => Iterator[A]) extends Iterator[A] {
  var currentIt = it

  def next() = {
    if (!currentIt.hasNext)
      currentIt = it
    currentIt.next()
  }

  def hasNext = true
}

object CircularIterator {
  def apply[A](it: => Iterator[A]) =
    new CircularIterator(it)
}
