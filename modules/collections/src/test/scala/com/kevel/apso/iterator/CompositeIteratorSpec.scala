package com.kevel.apso.iterator

import org.specs2.mutable.*

@deprecated("The stack overflow caused by Iterator.++ should be fixed in recent Scala versions.", "0.15.0")
class CompositeIteratorSpec extends Specification {
  "A composite log" should {
    def a1 = Iterator(1, 2, 3)
    def a2 = Iterator.empty[Int]
    def a3 = Iterator(4, 5, 6)

    val concatExpected = List(1, 2, 3, 4, 5, 6)

    "combine two iterators correctly" in {
      val cit = CompositeIterator(a1, a2, a3)
      cit.toList === concatExpected
    }

    "apply correctly concatenation (++)" in {
      val cit = CompositeIterator[Int]()
      (cit ++ a1 ++ a2 ++ a3).toList === concatExpected
    }

    "be bufferable (head)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.head === concatExpected.head
      cit.toList === concatExpected
    }

    "be bufferable (filter)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.dropWhile(_ < 3).toList === concatExpected.dropWhile(_ < 3)
    }

    "be bufferable (takeWhile)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.filter(x => x < 5).toList === concatExpected.filter(x => x < 5)
    }

    "be bufferable (dropWhile)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.filter(x => x > 2 && x < 5).toList === concatExpected.filter(x => x > 2 && x < 5)
    }
  }
}
