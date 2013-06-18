package eu.shiftforward.apso

import scala.compat.Platform

object Implicits {

  final implicit class ApsoString(val s: String) extends AnyVal {

    /**
     * Enumerates all the strings of a given length using the characters of this string as alphabet.
     * @param n the number of letters of each returned string
     * @return a sequence of strings of length `n` consisting of characters from this string.
     */
    def enumerate(n: Int): IndexedSeq[String] = {
      val alphabet = s.split("").filterNot(_.isEmpty).toIndexedSeq

      Iterable.fill(n)(alphabet) reduceLeft { (a, b) => for (a <- a; b <- b) yield a + b }
    }

    /**
     * Pads this string to a given length.
     * @param length the length to which this string is to be padded
     * @param ch the character used to fill the string
     * @return the padded string.
     */
    def padLeft(length: Int, ch: Char) = {
      val sb = new StringBuilder(length)
      (1 to length - s.length).foreach { i => sb.append(ch) }
      sb.append(s).toString()
    }

    /**
     * Returns the UTF-8 byte array representation of this string with a trailing zero byte.
     * @return the UTF-8 byte array representation of this string with a trailing zero byte.
     */
    def getBytesWithNullTerminator: Array[Byte] = {
      val stringBytes = s.getBytes("UTF-8")
      val buffer = new Array[Byte](stringBytes.size + 1)
      Platform.arraycopy(stringBytes, 0, buffer, 0, stringBytes.size)
      buffer
    }
  }

  final implicit class ApsoSeq[T](val seq: Seq[T]) extends AnyVal {

    /**
     * Partitions this sequence into a given number of subsequences. It is guaranteed that
     * the sequence is split into subsquences as even as possible; if the split is uneven,
     * the first `this.length % n` subsequences contain one more element than the remaining
     * ones.
     * @param n the number of subsequences into which this sequence must be split
     * @return a new sequence of `n` subsequences of this sequence.
     */
    def split(n: Int): IndexedSeq[Seq[T]] = {
      val q = seq.length / n
      val r = seq.length % n
      val indices = for (i <- 0 to n) yield q * i + math.min(i, r)
      for (i <- 0 until n) yield seq.slice(indices(i), indices(i + 1))
    }

    /**
     * Returns a subsequence of this sequence based on a percentage of the total number of elements.
     * @param percentage the percentage of elements of this sequence that the returned sequence must contain
     * @return a subsequence of this sequence based on a percentage of the total number of elements.
     */
    def sample(percentage: Double): Seq[T] =
      seq.take((seq.length * percentage).toInt)
  }

  final implicit class ApsoMap[A, B](val map: Map[A, B]) extends AnyVal {

    /**
     * Merges a given map into this map. The map is constructed as follows:
     * <ul>
     *   <li>Keys present in this map but not in `that` map are present in the merged map;
     *   <li>Keys present in both maps are present in the merged map with a value given by `f(thisValue, thatValue)`;
     *   <li>Keys present in `that` map but not in this map are <b>not</b> present in the merged map.
     * </ul>
     * @param that the map to be merged into this map
     * @param f the function used to merge two values with the same key
     * @return the merged map.
     */
    def merge(that: Map[A, B])(f: (B, B) => B): Map[A, B] =
      map.foldLeft(map) {
        case (acc, (key, value)) =>
          if (that.contains(key)) acc + (key -> f(value, that(key))) else acc
      }

    /**
     * Merges a given map with this map. The map is constructed as follows:
     * <ul>
     *   <li>Keys present in one of thw two maps are present in the merged map;
     *   <li>Keys in both maps are present in the merged map with a value given by `f(thisValue, thatValue)`;
     * </ul>
     * @param that the map to be merged with this map
     * @param f the function used to merge two values with the same key
     * @return the merged map.
     */
    def twoWayMerge(that: Map[A, B])(f: (B, B) => B): Map[A, B] =
      map.foldLeft(that) {
        case (thatMap, (key, mapValue)) =>
          thatMap.get(key) match {
            case Some(thatValue) => thatMap.updated(key, f(mapValue, thatValue))
            case None => thatMap.updated(key, mapValue)
          }
      }
  }

  final implicit class ApsoListMap[K, V](val list: Seq[Map[K, V]]) extends AnyVal {

    /**
     * Converts this list of maps into a map of lists. The order of the elements is kept between
     * structures. If a zero element is given, maps which do not contain certain keys are filled with
     * the zero element, which effectively implies that all the lists in the given map will have the
     * same length, corresponding to the size of the set of all keys. If a zero element is not given,
     * only the elements present in this map are packed into the lists of the resulting map.
     * @param zero the zero element, used as described above
     * @return the map of lists converted from this map.
     */
    def sequenceOnMap(zero: Option[V] = None): Map[K, List[V]] = {
      lazy val default = zero.toList

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

    /**
     * Uses this resouce and closes it afterwards.
     * @param f the block of code to execute using this resource
     * @tparam T the return type of the code block.
     * @return the value returned by the code block.
     */
    def use[T](f: U => T): T =
      try { f(res) } finally { res.close() }
  }
}
