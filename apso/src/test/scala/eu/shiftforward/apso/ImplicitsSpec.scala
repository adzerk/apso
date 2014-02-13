package eu.shiftforward.apso

import org.specs2.mutable._
import eu.shiftforward.apso.Implicits._
import scala.util.Random

class ImplicitsSpec extends Specification {

  "An ApsoTraversableOnce" should {
    "calculate correctly the average of a list of integral values" in {
      List[Byte](1, 4, 3, 2, 5).average === 3
      List[Short](2, 8, 6, 4, 10).average === 6
      List[Int](3, 12, 9, 6, 15).average === 9
      List[Long](4, 16, 12, 8, 20).average === 12
      List[Byte](1, 2).average === 1
      List[Int](1, 2).average === 1
      List[Short](1, 2).average === 1
      List[Long](1, 2).average === 1
    }

    "calculate correctly the average of a list of fractional values" in {
      List[Float](1, 4, 3, 2, 5).average === 3.0
      List[Double](2, 8, 6, 4, 10).average === 6.0
      List[Float](1, 2).average === 1.5
      List[Double](1, 2).average === 1.5
    }

    "throw an exception when the average is called on an empty traversable" in {
      List[Int]().average must throwA[IllegalArgumentException]
    }
  }

  "An ApsoMap" should {
    "support the mapKeys method" in {
      val m = Map(1 -> 2, 3 -> 4, 5 -> 6, 7 -> 8)
      m.mapKeys(_ * 2) ===
        Map(2 -> 2, 6 -> 4, 10 -> 6, 14 -> 8)
    }
  }

  "An ApsoListMap" should {
    "convert correctly a list of maps into a map of lists" in {
      List[Map[Int, Int]]().sequenceOnMap() === Map[Int, List[Int]]()

      List(Map(1 -> 2), Map(1 -> 3), Map(2 -> 3)).sequenceOnMap() ===
        Map(1 -> List(2, 3), 2 -> List(3))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a")).sequenceOnMap() ===
        Map(1 -> List("c"), 2 -> List("b"), 3 -> List("a"))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a"), Map(1 -> "c2", 4 -> "aa")).sequenceOnMap() ===
        Map(1 -> List("c", "c2"), 2 -> List("b"), 3 -> List("a"), 4 -> List("aa"))
    }

    "convert correctly a list of maps into a map of lists with a zero value" in {
      List[Map[Int, Int]]().sequenceOnMap(Some(0)) === Map[Int, List[Int]]()

      List(Map(1 -> 2), Map(1 -> 3), Map(2 -> 3)).sequenceOnMap(Some(0)) ===
        Map(1 -> List(2, 3, 0), 2 -> List(0, 0, 3))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a")).sequenceOnMap(Some("")) ===
        Map(1 -> List("c"), 2 -> List("b"), 3 -> List("a"))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a"), Map(1 -> "c2", 4 -> "aa")).sequenceOnMap(Some("")) ===
        Map(1 -> List("c", "c2"), 2 -> List("b", ""), 3 -> List("a", ""), 4 -> List("", "aa"))
    }
  }

  "An ApsoRandom" should {
    class MockRandom(elems: Double*) extends Random {
      private[this] var nextElems = elems.toList
      override def nextDouble(): Double = { val e = nextElems.head; nextElems = nextElems.tail; e }
    }

    "select correctly an element from a sequence using weights" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val list = List(("a", 0.5), ("b", 0.25), ("c", 0.25))

      rand.weightedChoice[(String, Double)](list, _._2) === Some("a", 0.5)
      rand.weightedChoice[(String, Double)](list, _._2) === Some("c", 0.25)
      rand.weightedChoice[(String, Double)](list, _._2) === Some("b", 0.25)
    }

    "select correctly an element from a sequence in case of underweighting" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val list = List(("a", 0.5), ("b", 0.25))

      rand.weightedChoice[(String, Double)](list, _._2) === Some("a", 0.5)
      rand.weightedChoice[(String, Double)](list, _._2) === None
      rand.weightedChoice[(String, Double)](list, _._2) === Some("b", 0.25)
    }

    "select an element from a sequence with a custom weight range" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val list = List(("a", 0.5), ("b", 0.5), ("c", 0.5))

      rand.weightedChoice[(String, Double)](list, _._2, rand.nextDouble() * 1.5) === Some("a", 0.5)
      rand.weightedChoice[(String, Double)](list, _._2, rand.nextDouble() * 1.5) === Some("c", 0.5)
      rand.weightedChoice[(String, Double)](list, _._2, rand.nextDouble() * 1.5) === Some("c", 0.5)
    }

    "select a random element using reservoir sampling" in {
      val rand = new MockRandom(0.4, 0.2, 0.8, 0.3, 0.7, 0.3, 0.9, 0.45, 0.1, 0.35, 0.9, 0.9)
      val list = List("a", "b", "c")

      rand.reservoirSample(Nil) === None
      rand.reservoirSample(list) === Some("b") // 0.2 < 1/2 in (a, b), 0.8 > 1/3 in (b, c)
      rand.reservoirSample(list) === Some("c") // 0.7 > 1/2 in (a, b), 0.3 < 1/3 in (a, c)
      rand.reservoirSample(list) === Some("c") // 0.45 < 1/2 in (a, b), 0.1 < 1/3 in (b, c)
      rand.reservoirSample(list) === Some("a") // 0.9 > 1/2 in (a, b), 0.9 > 1/3 in (a, c)
    }
  }
}
