package eu.shiftforward.apso.collection

import org.specs2.mutable._

class DeboxMapSpec extends Specification {

  "A DeboxMap" should {

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
  }

}
