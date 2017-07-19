package eu.shiftforward.apso.iterator

import org.specs2.mutable._

@deprecated("This will be removed in a future version", "2017/07/13")
class ExtendedIteratorSpec extends Specification {

  "An ExtendedIterator" should {

    "allow setting hooks for when the iterator has no more elements" in {
      val it = new ExtendedIterator(Iterator(1, 2))
      var wasCalled = false
      it.onEnd { wasCalled = true }

      it.hasNext must beTrue
      wasCalled must beFalse
      it.next() mustEqual 1

      it.hasNext must beTrue
      wasCalled must beFalse
      it.next() mustEqual 2

      it.hasNext must beFalse
      wasCalled must beTrue
    }
  }
}
