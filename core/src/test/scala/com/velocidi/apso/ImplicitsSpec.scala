package com.velocidi.apso

import scala.util.Random

import org.specs2.ScalaCheck
import org.specs2.mutable._

import com.velocidi.apso.Implicits._

class ImplicitsSpec extends Specification with ScalaCheck with FutureExtraMatchers {

  "An ApsoRandom" should {

    class MockRandom(elems: Double*) extends Random {
      private[this] var nextElems = elems.toList
      override def nextInt() = { val e = nextElems.head.toInt; nextElems = nextElems.tail; e }
      override def nextInt(n: Int) = { val e = nextElems.head.toInt % n; nextElems = nextElems.tail; e }
      override def nextDouble() = { val e = nextElems.head; nextElems = nextElems.tail; e }
    }

    def centralMoment(n: Int, xs: Iterable[Double]) = {
      val avg = xs.sum / xs.size
      val ys = xs map { x: Double => math.pow(x - avg, n.toDouble) }
      ys.sum / ys.size
    }

    "select correctly an element from an indexed sequence" in {
      val rand = new MockRandom(1, 4, 3, 2)
      val seq = Vector("a", "b", "c", "d", "e")

      rand.choose(seq) === Some("b")
      rand.choose(seq) === Some("e")
      rand.choose(seq) === Some("d")
      rand.choose(Vector()) === None
    }

    "select correctly multiple elements from a sequence" in {
      val rand = Random
      rand.setSeed(0)
      val runs = 10000
      val n = 5
      val elems = Stream.iterate(0, 50) { _ + 1 }

      val tests = (1 to runs).map { _ => rand.chooseN(elems, n) }

      tests.map(_.size).toSet === Set(n)
      val elementCounts: Map[Int, Int] = tests.flatten.groupBy(identity).mapValues(_.size).toMap

      elementCounts.keys.size must beCloseTo(elems.size +/- 5)

      // Expected probability of a number being picked
      val prob = (1 to n)
        .foldLeft((elems.size, 0.0)) { case ((rem, acc), _) =>
          val newAcc = acc + (1.0 - acc) * (1.0 / (rem - 1))
          (rem - 1, newAcc)
        }
        ._2

      elementCounts.values.sum.toDouble / (elementCounts.size * runs) must beCloseTo(prob +/- 0.05)
    }

    "select correctly an element from a sequence using weights" in {
      val rand = new MockRandom(0.2, 0.8, 0.7, 0.1)
      val map = Map("a" -> 0.5, "b" -> 0.25, "c" -> 0.25)

      rand.monteCarlo(map) === Some("a")
      rand.monteCarlo(map) === Some("c")
      rand.monteCarlo(map) === Some("b")
      rand.monteCarlo(Map.empty[String, Double]) === None
    }

    "select correctly an element from a sequence using weights" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val map = Map("a" -> 0.5, "b" -> 0.25, "c" -> 0.25)

      rand.monteCarlo(map) === Some("a")
      rand.monteCarlo(map) === Some("c")
      rand.monteCarlo(map) === Some("b")
    }

    "select correctly an element from a sequence in case of underweighting" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val map = Map("a" -> 0.5, "b" -> 0.25)

      rand.monteCarlo(map) === Some("a")
      rand.monteCarlo(map) === None
      rand.monteCarlo(map) === Some("b")
    }

    "select an element from a sequence with a custom weight range" in {
      val rand = new MockRandom(0.2, 0.8, 0.7)
      val map = Map("a" -> 0.5, "b" -> 0.5, "c" -> 0.5)

      rand.monteCarlo(map, rand.nextDouble() * 1.5) === Some("a")
      rand.monteCarlo(map, rand.nextDouble() * 1.5) === Some("c")
      rand.monteCarlo(map, rand.nextDouble() * 1.5) === Some("c")
    }

    "select a random element using reservoir sampling" in {
      val rand = new MockRandom(0.4, 0.2, 0.8, 0.3, 0.7, 0.3, 0.9, 0.45, 0.1, 0.35, 0.9, 0.9)
      val list = List("a", "b", "c")

      rand.reservoirSample(list) === Some("b") // 0.2 < 1/2 in (a, b), 0.8 > 1/3 in (b, c)
      rand.reservoirSample(list) === Some("c") // 0.7 > 1/2 in (a, b), 0.3 < 1/3 in (a, c)
      rand.reservoirSample(list) === Some("c") // 0.45 < 1/2 in (a, b), 0.1 < 1/3 in (b, c)
      rand.reservoirSample(list) === Some("a") // 0.9 > 1/2 in (a, b), 0.9 > 1/3 in (a, c)
      rand.reservoirSample(Nil) === None
    }

    "provide a stream of samples with a given distribution" in {
      val rand = Random
      val map = Map("a" -> 0.2, "b" -> 0.3, "c" -> 0.5)
      val runs = 10000

      rand.samples(Map.empty[String, Double]) must beEmpty

      val sampleDistr = rand.samples(map).take(runs).foldLeft(Map.empty[String, Int]) { case (acc, k) =>
        acc.updated(k, acc.getOrElse(k, 0) + 1)
      }

      forall(map) { case (k, prob) =>
        sampleDistr(k).toDouble must beCloseTo(runs * prob, runs * prob * 0.1)
      }
    }

    "provide an ordered uniform distribution as a stream" in {
      val rand = Random
      rand.setSeed(0)
      val runs = 1000000
      val epsilon = 0.005
      val stream = rand.increasingUniformStream(runs).toList

      "the results must be ordered" in {
        stream.sorted === stream
      }

      "the average must be close to 0.5" in {
        stream.sum / runs must beCloseTo(0.5 +/- epsilon)
      }

      "the variance must be close to 1/12" in {
        centralMoment(2, stream) must beCloseTo(1.0 / 12 +/- epsilon)
      }

      "the skewness must be close to 0" in {
        centralMoment(3, stream) must beCloseTo(0.0 +/- epsilon)
      }
    }
  }
}
