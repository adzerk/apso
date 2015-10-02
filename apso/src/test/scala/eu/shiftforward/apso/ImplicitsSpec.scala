package eu.shiftforward.apso

import org.specs2.mutable._
import eu.shiftforward.apso.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class ImplicitsSpec extends Specification with FutureExtraMatchers {

  "An ApsoSeq" should {

    "merge two sorted collections" in {
      List(1, 5, 6).mergeSorted[Int, List[Int]](List.empty[Int]) === List(1, 5, 6)
      List.empty[Int].mergeSorted(List(2, 4)) === List(2, 4)
      Seq(2).mergeSorted(Seq(5)) === List(2, 5)
      Seq(5).mergeSorted(Seq(2)) === List(2, 5)

      List(1, 3, 5).mergeSorted(Stream(2, 4)) === List(1, 2, 3, 4, 5)
      List(1, 3, 5).mergeSorted(Stream(2, 4)) must beAnInstanceOf[List[_]]

      Stream(1, 3, 5).mergeSorted(List(2, 4)) === Stream(1, 2, 3, 4, 5)
      Stream(1, 3, 5).mergeSorted(List(2, 4)) must beAnInstanceOf[Stream[_]]

      trait Base { val x: Int }
      case class Impl1(x: Int) extends Base
      case class Impl2(x: Int) extends Base

      implicit def ord[T <: Base] = new Ordering[T] {
        override def compare(a: T, b: T): Int = a.x - b.x
      }

      Seq(Impl1(3)).mergeSorted(Seq(Impl2(5))) === Seq(Impl1(3), Impl2(5))
      Seq(Impl1(3)).mergeSorted(Seq(Impl2(5))) must beAnInstanceOf[Seq[Base]]
    }

    "take the n smallest/largest values" in {
      List(1, 4, 3, 2, 5).takeSmallest(2) === Set(1, 2)
      List(1, 3, 9, 7, 7).takeLargest(2) === Set(9, 7)
      List(1, 9, 4, 3, 2, 5, -1).takeSmallest(3) === Set(1, -1, 2)
      List().takeSmallest(5) === List()
      List(1, 4, 3, 2, 5).takeLargest(10) === Set(1, 4, 3, 2, 5)
      List(1, 4, 3, 2, 5).takeLargest(0) === Set()
    }
  }

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

    "have a bufferedTakeWhile operation" in {
      val iter = List(5, 6, 7, 8, 9, 10).iterator.buffered
      iter.bufferedTakeWhile(_ < 4).toList === List()
      iter.toList == List(5, 6, 7, 8, 9, 10)

      val iter2 = List(5, 6, 7, 8, 9, 10).iterator.buffered
      iter2.bufferedTakeWhile(_ < 7).toList === List(5, 6)
      iter2.bufferedTakeWhile(_ < 10).toList === List(7, 8, 9)
      iter2.toList === List(10)
    }
  }

  "An ApsoMap" should {

    "support the merge method" in {
      val m1 = Map(1 -> 1, 2 -> 2, 3 -> 3)
      val m2 = Map(3 -> 3, 4 -> 4, 5 -> 5)

      m1.merge(m2)(_ + _) ===
        Map(1 -> 1, 2 -> 2, 3 -> 6)
    }

    "support the twoWayMerge method" in {
      val m1 = Map(1 -> 1, 2 -> 2, 3 -> 3)
      val m2 = Map(3 -> 3, 4 -> 4, 5 -> 5)

      m1.twoWayMerge(m2)(_ + _) ===
        Map(1 -> 1, 2 -> 2, 3 -> 6, 4 -> 4, 5 -> 5)
    }

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

  "An ApsoOptionalFuture" should {

    "fallback on None" in {
      Future.successful(None).ifNoneOrErrorFallbackTo(Future.successful(Some(()))).await(1.second) must beSome
    }

    "fallback on Exception" in {
      Future.failed(new Exception).ifNoneOrErrorFallbackTo(Future.successful(Some(()))).await(1.second) must beSome
    }

    "don't fallback on Some" in {
      Future.successful(Some(1)).ifNoneOrErrorFallbackTo(Future.successful(Some(2))).await(1.second) must beSome(1)
    }

  }

  "An ApsoRandom" should {

    class MockRandom(elems: Double*) extends Random {
      private[this] var nextElems = elems.toList
      override def nextInt() = { val e = nextElems.head.toInt; nextElems = nextElems.tail; e }
      override def nextInt(n: Int) = { val e = nextElems.head.toInt % n; nextElems = nextElems.tail; e }
      override def nextDouble() = { val e = nextElems.head; nextElems = nextElems.tail; e }
    }

    "select correctly an element from a sequence" in {
      val rand = new MockRandom(1, 4, 3, 2)
      val seq = Vector("a", "b", "c", "d", "e")

      rand.choose(seq) === Some("b")
      rand.choose(seq) === Some("e")
      rand.choose(seq) === Some("d")
      rand.choose(Vector()) === None
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

      val sampleDistr = rand.samples(map).take(runs).
        foldLeft(Map.empty[String, Int]) { case (acc, k) => acc.updated(k, acc.getOrElse(k, 0) + 1) }

      forall(map) {
        case (k, prob) => sampleDistr(k).toDouble must beCloseTo(runs * prob, runs * prob * 0.1)
      }
    }

    "provide an ordered uniform distribution as a stream" in {
      val rand = Random
      rand.setSeed(0)
      val runs = 100000
      val epsilon = 0.001
      val stream = rand.increasingUniformStream(runs).toList

      def centralMoment(n: Int, xs: List[Double]) = {
        val avg = xs.sum / xs.size
        val ys = xs map { x: Double => math.pow(x - avg, n.toDouble) }
        ys.sum / ys.size
      }

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
