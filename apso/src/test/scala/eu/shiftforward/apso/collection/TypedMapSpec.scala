package eu.shiftforward.apso.collection

import org.specs2.matcher.Matcher
import org.specs2.mutable._

class TypedMapSpec extends Specification {

  def beTypedAs[A]: Matcher[A] = { a: A => ok }

  "A TypedMap" should {

    "have a working insert" in {
      val m1 = TypedMap.empty[Any]
      m1.contains[Int] === false
      m1.get[Int] === None
      m1.getOrElse[Int](-1) === -1
      m1.size === 0

      val m2 = m1 + 3
      m2.contains[Int] === true
      m2[Int] === 3
      m2.get[Int] === Some(3)
      m2.get[String] === None
      m2.getOrElse[Int](-1) === 3
      m2.getOrElse[String]("NULL") === "NULL"
      m2.size === 1

      val m3 = m2 + 4
      m3.contains[Int] === true
      m3[Int] === 4
      m3.size === 1

      val m4 = m3 + List("STR")
      m4.contains[List[String]] === true
      m4[List[String]] === List("STR")
      m4.size === 2
    }

    "have a working remove" in {
      val m = TypedMap.empty + 2
      m.size === 1
      m.-[Int].size === 0
    }

    "have correct types" in {
      TypedMap.empty must beTypedAs[TypedMap[_]]
      TypedMap.empty[Int] must beTypedAs[TypedMap[Int]]
      (TypedMap.empty + 2) must beTypedAs[TypedMap[Int]]
      TypedMap(2) must beTypedAs[TypedMap[Int]]
      (TypedMap.empty + 2 + "asd") must beTypedAs[TypedMap[Any]]
      TypedMap(2, "asd") must beTypedAs[TypedMap[Any]]
      (TypedMap.empty + List(2) + List(3)) must beTypedAs[TypedMap[List[Int]]]
      TypedMap(List(2), List(3)) must beTypedAs[TypedMap[List[Int]]]
      (TypedMap.empty + List(2) + List("asd")) must beTypedAs[TypedMap[List[Any]]]
      TypedMap(List(2), List("asd")) must beTypedAs[TypedMap[List[Any]]]
    }
  }

}
