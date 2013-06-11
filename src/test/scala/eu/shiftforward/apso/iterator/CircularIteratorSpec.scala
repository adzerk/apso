package eu.shiftforward.apso.iterator

import org.specs2.mutable._

class CircularIteratorSpec extends Specification {
  "A CircularIterator" should {
    "allow to iterate through elements in a circular fashion" in {
      val it = CircularIterator(Iterator(1, 2, 3))
      it.take(1).toList mustEqual List(1)
      it.take(2).toList mustEqual List(2, 3)
      it.take(3).toList mustEqual List(1, 2, 3)
      it.take(4).toList mustEqual List(1, 2, 3, 1)
      it.take(5).toList mustEqual List(2, 3, 1, 2, 3)
      it.take(6).toList mustEqual List(1, 2, 3, 1, 2, 3)
      it.take(7).toList mustEqual List(1, 2, 3, 1, 2, 3, 1)
      it.take(8).toList mustEqual List(2, 3, 1, 2, 3, 1, 2, 3)
      it.take(9).toList mustEqual List(1, 2, 3, 1, 2, 3, 1, 2, 3)
    }
  }
}
