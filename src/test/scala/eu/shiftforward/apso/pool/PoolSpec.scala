package eu.shiftforward.apso.pool

import org.specs2.mutable._

class PoolSpec extends Specification {
  "UnrestrictedPool" should {
    "acquire & release" in {
      class Foo()
      val pool = UnrestrictedPool(new Foo)
      val f1 = pool.acquire() // keep the reference
      pool.release(f1)
      pool.acquire() must beTheSameAs(f1) // reference equality
      pool.acquire() must not beTheSameAs (f1)
    }
  }

  "SimplePool" should {
    "acquire & release" in {
      case class Foo(i: Int)
      val pool = SimplePool[Foo, Int](Foo(_), (f, i) => f.i == i)
      val f1 = pool.acquire(1)
      pool.release(f1)
      pool.acquire(1) must beTheSameAs(f1) // reference equality
      pool.acquire(1) must not beTheSameAs (f1)
    }

    "respect the key function" in {
      case class Foo(i: Int)
      val pool = SimplePool[Foo, Int](Foo(_), (f, i) => f.i == i)
      val f1 = pool.acquire(1)
      f1.i === 1
    }
  }
}
