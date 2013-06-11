package eu.shiftforward.apso.iterator

import org.specs2.mutable._

class RoundRobinIteratorSpec extends Specification {
  "A RoundRobinIterator" should {
    "take elements from each iterator in order" in {
      val it = RoundRobinIterator(() => Iterator(1, 2, 3), () => Iterator(1, 2), () => Iterator(1))
      val el = it.toList

      el mustEqual List(1, 1, 1, 2, 2, 3)
    }

    "work with empty iterators" in {
      val it = RoundRobinIterator(() => Iterator(1, 2, 3), () => Iterator(), () => Iterator(1))
      val el = it.toList

      el mustEqual List(1, 1, 2, 3)
    }

    "apply correct concatenation" in {
      val it = RoundRobinIterator() ++ Iterator(1, 2, 3) ++ Iterator(1, 2) ++ Iterator(1)
      val el = it.toList

      el mustEqual List(1, 1, 1, 2, 2, 3)
    }
  }
}
