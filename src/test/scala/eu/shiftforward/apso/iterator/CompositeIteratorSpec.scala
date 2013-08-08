package eu.shiftforward.apso.iterator

import org.specs2.mutable._

class CompositeIteratorSpec extends Specification {
  isolated

  "A composite log" should {
    val a1 = Iterator(1, 2, 3)
    val a2 = Iterator()
    val a3 = Iterator(4, 5, 6)

    val concatExpected = List(1, 2, 3, 4, 5, 6)

    "combine two iterators correctly" in {
      val cit = CompositeIterator(a1, a2, a3)
      cit.toList mustEqual concatExpected
    }

    "apply correctly concatenation (++)" in {
      val cit = CompositeIterator[Int]()
      (cit ++ a1 ++ a2 ++ a3).toList mustEqual concatExpected
      ok
    }

    "correctly serialize iterators to a single queue" in {

      "not create a recursive structure" in {
        val cit = CompositeIterator() ++ a1 ++ a2 ++ a3
        cit.iterators.length mustEqual 3
      }

      "not encapsulate multiple instances of composite iterators" in {
        val cit = CompositeIterator(a1) ++ CompositeIterator(a2) ++ CompositeIterator(a3)
        cit.iterators must haveAllElementsLike {
          case it: CompositeIterator[_] => ko
          case _ => ok
        }
      }
    }
  }
}
