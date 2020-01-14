package com.velocidi.apso.collection

import scala.reflect.ClassTag

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import org.specs2.mutable._

class DeboxMapSpec extends Specification with ScalaCheck {
  type K = String
  type V = Int

  def genDeboxMap[A: Arbitrary: ClassTag, B: Arbitrary: ClassTag]: Gen[DeboxMap[A, B]] = {
    import org.scalacheck.Gen._
    for {
      keys <- listOf(arbitrary[A])
      values <- listOfN(keys.size, arbitrary[B])
      removedKeys <- someOf(keys)
    } yield if (keys.isEmpty)
      DeboxMap.empty[A, B]
    else {
      val map = DeboxMap[A, B](keys.toArray, values.toArray)
      // Remove some keys to have dirty blocks (state = 2)
      removedKeys.foreach { k => map.remove(k) }
      map
    }
  }

  implicit def arbDeboxMap[A: Arbitrary: ClassTag, B: Arbitrary: ClassTag] = Arbitrary(genDeboxMap[A, B])

  "A DeboxMap" should {

    "support the get, contains and equals methods" in forAll { (entries: Map[K, V]) =>
      val map = DeboxMap.empty[K, V]
      entries.foreach { case (k, v) => map.update(k, v) }
      entries.forall {
        case (k, _) =>
          map.contains(k) == entries.contains(k) &&
            map.get(k) == entries.get(k)
      } && map.equals(map) && !map.equals(0)
    }

    "support the update method" in forAll { (map: DeboxMap[K, V], k: K, v: V) =>
      map.update(k, v)
      map.contains(k) && map.get(k) == Some(v)
    }

    "support the remove method" in forAll { (map: DeboxMap[K, V], k: K) =>
      map.remove(k)
      !map.contains(k) && map.get(k) == None
    }

    "support the remove and length methods" in forAll { (map: DeboxMap[K, V], k: K) =>
      val initialLength = map.length
      if (map.contains(k)) {
        map.remove(k)
        map.length == initialLength - 1
      } else {
        map.remove(k)
        map.length == initialLength
      }
    }

    "support the update and length methods" in forAll { (map: DeboxMap[K, V], k: K) =>
      val initialLength = map.length
      if (map.contains(k)) {
        map.update(k, 0)
        map.length == initialLength
      } else {
        map.update(k, 0)
        map.length == initialLength + 1
      }
    }

    "support the getOrElse method" in forAll { (map: DeboxMap[K, V], k: K, v: V) =>
      (map.contains(k) && (map.getOrElse(k, v) == map.get(k).get)) ||
        (!map.contains(k) && (map.getOrElse(k, v) == v))
    }

    "support the getOrElseUpdate method" in forAll { (map: DeboxMap[K, V], k: K, v: V) =>
      if (map.contains(k)) {
        val oldValue = map.get(k).get
        map.getOrElseUpdate(k, v) == oldValue
      } else {
        map.getOrElseUpdate(k, v) == v && map.contains(k) && map.get(k).get == v
      }
    }

    "support the foreach method" in forAll { (map: DeboxMap[K, V]) =>
      val entries = scala.collection.mutable.ListBuffer.empty[(K, V)]
      map.foreach { case (k, v) => entries.append((k, v)) }
      entries.length == map.length && entries.forall {
        case (k, v) =>
          map.get(k) == Some(v)
      }
    }

    "have a working apply" in {
      val m = DeboxMap.empty[Int, Int]
      m.contains(2) === false
      m.get(2) === None
      m.getOrElse(2, -1) === -1
      m.get(5) === None
      m.length === 0

      m(2) = 3
      m.contains(2) === true
      m(2) === 3
      m.get(2) === Some(3)
      m.get(5) === None
      m.getOrElse(2, -1) === 3
      m.getOrElse(5, -1) === -1
      m.length === 1

      m(2) = 4
      m.contains(2) === true
      m(2) === 4
      m.get(2) === Some(4)
      m.get(5) === None
      m.getOrElse(2, -1) === 4
      m.getOrElse(5, -1) === -1
      m.length === 1

      m(5) = 6
      m.contains(5) === true
      m(5) === 6
      m.get(2) === Some(4)
      m.get(5) === Some(6)
      m.getOrElse(2, -1) === 4
      m.getOrElse(5, -1) === 6
      m.length === 2

      m.remove(5)
      m.contains(5) === false
      m.get(2) === Some(4)
      m.get(5) === None
      m.getOrElse(2, -1) === 4
      m.getOrElse(5, -1) === -1
      m.length === 1
    }

    "have a working getOrElseUpdate" in {
      val m = DeboxMap.empty[Int, Int]
      m.getOrElseUpdate(2, 0) === 0
      m.contains(2) == true
      m(2) === 0

      m.getOrElseUpdate(2, 1) === 0
      m.remove(2)
      m.getOrElseUpdate(2, 1) === 1
    }

    "have a working resize/remove" in {
      val m = DeboxMap.empty[Int, Double]
      m.length === 0
      m.contains(9) === false
      m.contains(11) === false
      m.contains(100) === false
      for (i <- 0 until 100) {
        m(i) = i.toDouble * 7
      }
      m.length === 100
      m(9) === 63.0
      m(11) === 77.0
      m.contains(100) === false
      for (i <- 0 until 100) {
        m.remove(i)
      }
      m.length === 0
      m.contains(9) === false
      m.contains(11) === false
      m.contains(100) === false
      for (i <- 0 until 100) {
        m(i) = i.toDouble * 9
      }
      m.length === 100
      m(9) === 81.0
      m(11) === 99.0
      m.contains(100) === false
      for (i <- 0 until 100) {
        m.remove(i)
      }
      m.length === 0
      m.contains(9) === false
      m.contains(11) === false
      m.contains(100) === false
    }

    "have a working foreach" in {
      val m = DeboxMap.empty[Int, Int]
      val is = Vector.fill(10)(scala.util.Random.nextInt())

      is.foreach { i => m.update(i, i) }

      m.length === 10

      var sum = 0
      m.foreach { (_, v) => sum += v }

      sum === is.sum
    }

    "have a working map" in {
      val m = DeboxMap.empty[Int, Int]
      val is = Vector.fill(10)(scala.util.Random.nextInt(Int.MaxValue - 1))

      is.foreach { i => m.update(i, i) }

      m.length === 10

      val lst = m.map { (_, v) => v + 1 }

      lst.sum === is.sum + 10
    }

    "have a working equals" in {
      val m1 = DeboxMap.empty[Int, Int]
      val m2 = DeboxMap.empty[Int, Int]

      (1 to 10).foreach { i =>
        m1.update(i, i)
        m2.update(i, i)
      }

      m1 === m2

      m1.update(11, 11)

      m1 !== m2
    }

    "handle hash collisions" in {
      val str1 = "q978Q1iCaznURFPWnTy1"
      val str2 = "tW5V7aiYeLQ4ZrCUGJ1x"
      str1.## === str2.##

      val m1 = DeboxMap.empty[String, Int]
      m1.update(str1, 1)
      m1.update(str2, 2)

      m1.get(str1) === Some(1)
      m1.get(str2) === Some(2)

      m1.remove(str1)

      m1.get(str1) === None
      m1.get(str2) === Some(2)

      m1.update(str2, 3)

      m1.get(str1) === None
      m1.get(str2) === Some(3)

      m1.remove(str2)

      m1.get(str1) === None
      m1.get(str2) === None

      val m2 = DeboxMap.empty[String, Int]

      m2.getOrElseUpdate(str1, 1)
      m2.getOrElseUpdate(str2, 2)

      m2.getOrElse(str1, -1) === 1
      m2.getOrElse(str2, -1) === 2

      m2.remove(str1)

      m2.getOrElse(str1, -1) === -1
      m2.getOrElse(str2, -1) === 2

      m2.getOrElseUpdate(str2, 3)

      m2.getOrElse(str1, -1) === -1
      m2.getOrElse(str2, -1) === 2

      m2.remove(str2)

      m2.getOrElse(str1, -1) === -1
      m2.getOrElse(str2, -1) === -1
    }
  }

}
