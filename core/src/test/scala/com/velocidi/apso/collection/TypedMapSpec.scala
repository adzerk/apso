package com.velocidi.apso.collection

import org.specs2.matcher.Matcher
import org.specs2.mutable._

class TypedMapSpec extends Specification {

  def beTypedAs[A]: Matcher[A] = { a: A => ok }

  class Animal(val name: String)
  case class Dog(n: String) extends Animal(n)
  case class Cat(n: String) extends Animal(n)

  "A TypedMap" should {

    "have a working insert" in {
      val m1 = TypedMap.empty[Any]
      m1.contains[Int] === false
      m1.get[Int] === None
      m1.getOrElse[Int](-1) === -1
      m1.size === 0
      m1.values must beEmpty

      val m2 = m1 + 3
      m2.contains[Int] === true
      m2[Int] === 3
      m2.get[Int] === Some(3)
      m2.get[String] === None
      m2.getOrElse[Int](-1) === 3
      m2.getOrElse[String]("NULL") === "NULL"
      m2.size === 1
      m2.values must containTheSameElementsAs(List(3))

      val m3 = m2 + 4
      m3.contains[Int] === true
      m3[Int] === 4
      m3.size === 1
      m3.values must containTheSameElementsAs(List(4))

      val m4 = m3 + List("STR")
      m4.contains[List[String]] === true
      m4[List[String]] === List("STR")
      m4.size === 2
      m4.values must containTheSameElementsAs(List(4, List("STR")))
    }

    "have a working remove" in {
      val m = TypedMap.empty + 2
      m.size === 1
      m.-[Int].size === 0
      m.-[Int].values must beEmpty
    }

    "support type hierarchies" in {
      val m = TypedMap.empty[Animal]

      val m1 = m + Dog("Baco") + Cat("Tareco")
      m1[Dog].name === "Baco"
      m1[Cat].name === "Tareco"

      val m2 = m1 + Dog("Sherlock")
      m2[Dog].name === "Sherlock"

      val mm = TypedMap(Dog("Baco"), Cat("Tareco"))
      mm[Dog].name === "Baco"
      mm[Cat].name === "Tareco"
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

    "hava a working equals and hashCode methods" in {
      val m = TypedMap.empty[Animal]
      val m1 = m + Dog("Baco") + Cat("Tareco")
      val m2 = m1 + Dog("Sherlock")
      val m3 = m1 + Dog("Sherlock")

      m1.equals("string") must beFalse
      m1 !=== m2
      m1.hashCode !=== m2.hashCode
      m2 === m3
      m2.hashCode === m3.hashCode
    }
  }

}
