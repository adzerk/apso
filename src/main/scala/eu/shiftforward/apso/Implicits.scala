package eu.shiftforward.apso

import scala.compat.Platform

object Implicits {

  final implicit class ApsoString(val s: String) extends AnyVal {
    /**
     * Enumerates all strings of given length using this string as alphabet
     */
    def enumerate(n: Int): IndexedSeq[String] = {
      val alphabet = s.split("").filterNot(_.isEmpty).toIndexedSeq

      Iterable.fill(n)(alphabet) reduceLeft { (a, b) => for (a <- a; b <- b) yield a + b }
    }

    def padLeft(length: Int, ch: Char) = {
      val sb = new StringBuilder(length)
      (1 to (length - s.length)).foreach { i => sb.append(ch) }
      sb.append(s).toString
    }

    def getBytesWithNullTerminator: Array[Byte] = {
      val stringBytes = s.getBytes("UTF-8")
      val buffer = new Array[Byte](stringBytes.size + 1)
      Platform.arraycopy(stringBytes, 0, buffer, 0, stringBytes.size)
      buffer
    }
  }

  final implicit class ApsoSeq[T](val seq: Seq[T]) extends AnyVal {
    def split(n: Int): IndexedSeq[Seq[T]] = {
      val q = seq.length / n
      val r = seq.length % n
      val indices = for (i <- 0 to n) yield q * i + math.min(i, r)
      val slices = for (i <- 0 until n) yield seq.slice(indices(i), indices(i + 1))

      slices
    }

    def sample(percentage: Double): Seq[T] =
      seq.take((seq.length * percentage).toInt)
  }

  final implicit class ApsoMap[A, B](val map: Map[A, B]) extends AnyVal {
    def merge(that: Map[A, B])(f: (B, B) => B): Map[A, B] =
      map.foldLeft(map) {
        case (acc, (key, value)) =>
          if (that.contains(key)) acc + (key -> f(value, that(key))) else acc
      }
  }

  final implicit class ApsoListMap[K, V](val list: Seq[Map[K, V]]) extends AnyVal {
    def sequenceOnMap(zero: Option[V] = None): Map[K, List[V]] = {
      val default = zero.toList // TODO make this a lazy val once Scala allows them in value classes

      def construct(value: Option[V]) = value match {
        case None => default
        case Some(v) => List(v)
      }
      val keysOpt = list.view.map(_.keys).reduceOption(_ ++ _)

      keysOpt.map { keys =>
        list.foldLeft(Map[K, List[V]]()) { (acc, innerMap) =>
          keys.foldLeft(acc) {
            case (acc, key) =>
              acc + (key -> (acc.getOrElse(key, Nil) ::: construct(innerMap.get(key))))
          }
        }
      } getOrElse {
        Map()
      }
    }
  }

  final implicit class ApsoCloseable[U <: AutoCloseable](val res: U) extends AnyVal {
    def use[T](f: U => T): T =
      try { f(res) } finally { res.close() }
  }
}
