package com.kevel.apso.iterator

import org.specs2.mutable._

@deprecated("The stack overflow caused by Iterator.++ should be fixed in recent Scala versions.", "0.15.0")
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
    }

    "be bufferable (head)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.head === concatExpected.head
      cit.toList mustEqual concatExpected
    }

    "be bufferable (filter)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.dropWhile(_ < 3).toList mustEqual concatExpected.dropWhile(_ < 3)
    }

    "be bufferable (takeWhile)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.filter(x => x < 5).toList mustEqual concatExpected.filter(x => x < 5)
    }

    "be bufferable (dropWhile)" in {
      val cit = (CompositeIterator[Int]() ++ a1 ++ a2 ++ a3).buffered
      cit.filter(x => x > 2 && x < 5).toList mustEqual concatExpected.filter(x => x > 2 && x < 5)
    }
  }
}
