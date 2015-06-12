package eu.shiftforward.apso.collection

import org.specs2.mutable._

class TypedMapSpec extends Specification {

  "A TypedMap" should {

    "have a working insert" in {
      val m1 = TypedMap.empty
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

      val m4 = m3 + "STR"
      m4.contains[String] === true
      m4[String] === "STR"
      m4.size === 2
    }

    "have a working remove" in {
      val m = TypedMap.empty + 2
      m.size === 1
      m.-[Int].size === 0
    }
  }

}
