package com.velocidi.apso.iterator

import org.specs2.mutable._

import com.velocidi.apso.iterator.Implicits._

class ImplicitsSpec extends Specification {
  "An ApsoBufferedIterator" should {

    "merge sorted iterators" in {
      List(1, 5, 6).iterator.buffered.mergeSorted(List.empty[Int].iterator.buffered).toList === List(1, 5, 6)
      List.empty[Int].iterator.buffered.mergeSorted(List(2, 4).iterator.buffered).toList === List(2, 4)
      Seq(2).iterator.buffered.mergeSorted(Seq(5).iterator.buffered).toList === List(2, 5)
      Seq(5).iterator.buffered.mergeSorted(Seq(2).iterator.buffered).toList === List(2, 5)

      List(1, 3, 5).iterator.buffered.mergeSorted(Stream(2, 4).iterator.buffered).toList === List(1, 2, 3, 4, 5)

      Stream(1, 3, 5).iterator.buffered.mergeSorted(List(2, 4).iterator.buffered).toStream === Stream(1, 2, 3, 4, 5)

      trait Base { val x: Int }
      case class Impl1(x: Int) extends Base
      case class Impl2(x: Int) extends Base

      implicit def ord[T <: Base] = new Ordering[T] {
        override def compare(a: T, b: T): Int = a.x - b.x
      }

      Seq(Impl1(3)).iterator.buffered.mergeSorted(Seq(Impl2(5)).iterator.buffered).toSeq === Seq(Impl1(3), Impl2(5))
      Seq(Impl1(3)).iterator.buffered.mergeSorted(Seq(Impl2(5)).iterator.buffered).toSeq must beAnInstanceOf[Seq[Base]]

      List(1, 4, 7).iterator.buffered.mergeSorted(List(2, 5, 8).iterator.buffered).mergeSorted(List(3, 6, 9).iterator.buffered).toList ===
        List(1, 2, 3, 4, 5, 6, 7, 8, 9)
    }
  }
}
