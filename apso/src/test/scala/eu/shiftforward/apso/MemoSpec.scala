package eu.shiftforward.apso

import org.specs2.mutable._

class MemoizationSpec extends Specification {
  "A memoization decorator" should {
    "return the value of the function when called" in {
      val power = Memo { n: Int => n * n }
      power(2) === 4
      power(3) === 9
    }

    "cache once and return the cached result after the first invocation" in {
      val power = new Memo({ n: Int => n * n }) with MemoizationStats[Int, Int]
      power.size === 0
      (1 to 100).foreach { i => power(i); power(i) }
      power.size === 100
      power.misses === 100
      power.hits === 100
    }

    "clean when mandated to" in {
      val power = new Memo({ n: Int => n * n }) with MemoizationStats[Int, Int]
      power(2)
      power.size === 1
      power.misses === 1
      power.clear()
      power.size === 0
      power(2)
      power.size === 1
      power.misses === 2
    }
  }
}
