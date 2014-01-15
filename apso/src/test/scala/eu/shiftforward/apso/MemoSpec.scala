package eu.shiftforward.apso

import org.specs2.mutable._

class MemoizationSpec extends Specification {
  "A memoiziation decorator" should {
    "return the value of the function when called" in {
      val power = Memo { n: Int => n * n }
      power(2) === 4
      power(3) === 9
    }

    "don't grow with multiple invocations with the same parameters" in {
      val power = new Memo({ n: Int => n * n }) with MemoizationStats[Int, Int]
      power.size === 0
      (1 to 100).foreach { i => power(i) }
      power.size === 100
    }

    "clean when mandated to" in {
      val power = new Memo({ n: Int => n * n }) with MemoizationStats[Int, Int]
      power(2) === 4
      power.size === 1
      power.clear()
      power.size === 0
    }
  }
}
