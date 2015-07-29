package eu.shiftforward.apso

import akka.actor.ActorSystem
import akka.pattern.after
import eu.shiftforward.apso.collection.MergedBufferedIterator

import scala.collection.generic.CanBuildFrom
import scala.compat.Platform
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

/**
 * Object containing implicit classes and methods of general purpose.
 */
object Implicits {

  /**
   * Implicit class that provides new methods for any object.
   * @param obj the object to which the new methods are provided.
   */
  final implicit class ApsoAny[T](val obj: T) extends AnyVal {

    /**
     * Returns this object wrapped in a `Some`.
     * @return this object wrapped in a `Some`.
     */
    def some = Some(obj)
  }

  /**
   * Implicit class that provides new methods for strings.
   * @param s the string to which the new methods are provided.
   */
  final implicit class ApsoString(val s: String) extends AnyVal {

    /**
     * Enumerates all the strings of a given length using the characters of this
     * string as alphabet.
     * @param n the number of letters of each returned string
     * @return a sequence of strings of length `n` consisting of characters from
     *         this string.
     */
    def enumerate(n: Int): IndexedSeq[String] = {
      val alphabet = s.split("").filterNot(_.isEmpty).toIndexedSeq

      Iterable.fill(n)(alphabet) reduceLeft { (a, b) => for (a <- a; b <- b) yield a + b }
    }

    /**
     * Pads this string on the left to a given length.
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
     * Returns the UTF-8 byte array representation of this string with a
     * trailing zero byte.
     * @return the UTF-8 byte array representation of this string with a
     *         trailing zero byte.
     */
    def getBytesWithNullTerminator: Array[Byte] = {
      val stringBytes = s.getBytes("UTF-8")
      val buffer = new Array[Byte](stringBytes.size + 1)
      Platform.arraycopy(stringBytes, 0, buffer, 0, stringBytes.size)
      buffer
    }
  }

  /**
   * Implicit class that provides new methods for sequences.
   * @param seq the sequence to which the new methods are provided
   */
  final implicit class ApsoSeq[T](val seq: Seq[T]) extends AnyVal {

    /**
     * Partitions this sequence into a given number of subsequences. It is
     * guaranteed that the sequence is split into subsquences as even as
     * possible; if the split is uneven, the first `this.length % n`
     * subsequences contain one more element than the remaining ones.
     * @param n the number of subsequences into which this sequence must be
     *        split
     * @return a new sequence of `n` subsequences of this sequence.
     */
    def split(n: Int): IndexedSeq[Seq[T]] = {
      val q = seq.length / n
      val r = seq.length % n
      val indices = for (i <- 0 to n) yield q * i + math.min(i, r)
      for (i <- 0 until n) yield seq.slice(indices(i), indices(i + 1))
    }

    /**
     * Returns a subsequence of this sequence based on a percentage of the total
     * number of elements.
     * @param percentage the percentage of elements of this sequence that the
     *        returned sequence must contain
     * @return a subsequence of this sequence based on a percentage of the total
     *         number of elements.
     */
    def sample(percentage: Double): Seq[T] =
      seq.take((seq.length * percentage).toInt)
  }

  /**
   * Implicit class that provides new methods for sequences for which their concrete type is
   * important.
   * @param seq the sequence to which the new methods are provided
   * @tparam T the type of the elements in the sequence
   * @tparam CC the concrete type of the sequence
   */
  final implicit class ApsoSeqTyped[T, CC[X] <: Seq[X]](val seq: CC[T]) extends AnyVal {

    /**
     * Merges this sequence with another traversable assuming that both collections are already
     * sorted. This method eagerly evaluates all the elements of both collections, even if they are
     * lazy collections. For that reason, infinite sequences are not supported.
     * @param it the traversable collection to merge with this one
     * @param bf combiner factory which provides a combiner
     * @param ord the ordering with which the collections are sorted and with which the merged
     *            collection is to be returned
     * @tparam U element type of the resulting collection
     * @tparam That type of the resulting collection
     * @return this sequence merged with the given traversable
     */
    def mergeSorted[U >: T, That](it: TraversableOnce[U])(implicit bf: CanBuildFrom[CC[T], U, That], ord: Ordering[U]): That = {
      val b = bf(seq)

      if (seq.isEmpty) b ++= it
      else {
        val thisIt = seq.iterator
        var thisNext: T = thisIt.next()
        var finished = false

        for (elem <- it) {

          def takeFromThis() {
            if (!finished && ord.lt(thisNext, elem)) {
              b += thisNext
              if (thisIt.hasNext) thisNext = thisIt.next() else finished = true
              takeFromThis()
            }
          }
          takeFromThis()
          b += elem
        }

        if (!finished) {
          b += thisNext
          b ++= thisIt
        }
      }
      b.result()
    }
  }

  /**
   * Implicit class that provides new methods for traversable-once collections.
   * @param it the traversable-once collection to which the new methods are provided.
   */
  final implicit class ApsoTraversableOnce[T](val it: TraversableOnce[T]) extends AnyVal {

    /**
     * Returns the average of the elements of this collection.
     * @param num either an instance of `Numeric` or an instance of `Fractional`, defining a set of
     *            numeric operations which includes the `+` and the `/` operators to be used in
     *            forming the average.
     * @tparam A the result type of the `/` operator
     * @return the average of all elements of this collection with respect to the `+` and `/`
     *         operators in `num`.
     */
    def average[A >: T](implicit num: Numeric[A]): A = {
      val div: (A, A) => A = num match {
        case n: Fractional[A] => n.div
        case n: Integral[A] => n.quot
        case _ => sys.error("Numeric does not support division!")
      }
      val res = it.foldLeft(num.zero, num.zero) { (acc, e) =>
        (num.plus(acc._1, e), num.plus(acc._2, num.one))
      }
      if (res._2 == num.zero)
        throw new IllegalArgumentException("The traversable should not be empty!")
      div(res._1, res._2)
    }
  }

  /**
   * Implicit class that provides new methods for buffered iterators.
   * @param thisIt the buffered iterator to which the new methods are provided.
   */
  final implicit class ApsoBufferedIterator[T](val thisIt: BufferedIterator[T]) {

    /**
     * Lazily merges this buffered iterator with another buffered iterator assuming that both collections
     * are already sorted.
     * @param thatIt the iterator  to merge with this one
     * @param ord the ordering with which the collections are sorted and with which the merged
     *            collection is to be returned
     * @tparam U element type of the resulting collection
     * @return the merged iterators
     */
    def mergeSorted[U >: T](thatIt: BufferedIterator[U])(implicit ord: Ordering[U]): BufferedIterator[U] =
      MergedBufferedIterator(List(thatIt)).mergeSorted(thisIt)

    /**
     * Safer version of takeWhile that can be applied in succession
     * @param p predicate
     * @return iterator with the results
     */
    def bufferedTakeWhile(p: T => Boolean) = new BufferedIterator[T] {
      def hasNext = { thisIt.hasNext && p(thisIt.head) }
      def next() = (if (hasNext) thisIt else Iterator.empty.buffered).next()
      def head = thisIt.head
    }
  }

  /**
   * Implicit class that provides new methods for maps.
   * @param map the map to which the new methods are provided.
   */
  final implicit class ApsoMap[A, B](val map: Map[A, B]) extends AnyVal {

    /**
     * Merges a given map into this map. The map is constructed as follows:
     * <ul>
     *   <li>Keys present in this map but not in `that` map are present in the
     *       merged map;
     *   <li>Keys present in both maps are present in the merged map with a
     *       value given by `f(thisValue, thatValue)`;
     *   <li>Keys present in `that` map but not in this map are <b>not</b>
     *       present in the merged map.
     * </ul>
     * @param that the map to be merged into this map
     * @param f the function used to merge two values with the same key
     * @return the merged map.
     * @todo check if this method is really useful / needed.
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
     *   <li>Keys in both maps are present in the merged map with a value given
     *       by `f(thisValue, thatValue)`;
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

    /**
     * Applies a given function to all keys of this map. In case `f` is not
     * injective, the behaviour is undefined.
     * @param f the function to apply to all keys of this map
     * @return the resulting map with the keys mapped with function `f`.
     */
    def mapKeys[C](f: A => C): Map[C, B] =
      map.map({ case (k, v) => f(k) -> v }).toMap
  }

  /**
   * Implicit class that provides new methods for sequences of maps.
   * @param list the sequence of maps to which the new methods are provided.
   */
  final implicit class ApsoListMap[K, V](val list: Seq[Map[K, V]]) extends AnyVal {

    /**
     * Converts this list of maps into a map of lists. The order of the elements
     * is kept between structures. If a zero element is given, maps which do not
     * contain certain keys are filled with the zero element, which effectively
     * implies that all the lists in the given map will have the same length,
     * corresponding to the size of the set of all keys. If a zero element is
     * not given, only the elements present in this map are packed into the
     * lists of the resulting map.
     * @param zero the zero element, used as described above
     * @return the map of lists converted from this map.
     */
    def sequenceOnMap(zero: Option[V] = None)(implicit dummy: DummyImplicit): Map[K, List[V]] =
      sequenceOnMap(zero.map(v => { _: Map[K, V] => v }))

    /**
     * Converts this list of maps into a map of lists. The order of the elements
     * is kept between structures. If a zero element is given, maps which do not
     * contain certain keys are filled with the zero element, which is created by passing
     * the whole original map to the zero function. This implies that all the lists in
     * the given map will have the same length, corresponding to the size of the set of
     * all keys. If a zero element is not given, only the elements present in this map
     * are packed into the lists of the resulting map.
     *
     * @param zero a function that creates the zero element, based on the original map
     *             being processed.
     * @return the map of lists converted from this map.
     */
    def sequenceOnMap(zero: Option[Map[K, V] => V]): Map[K, List[V]] = {
      def construct(value: Option[V], zero: => Option[V]) = value match {
        case None => zero.toList
        case Some(v) => List(v)
      }

      val keysOpt = list.view.map(_.keys).reduceOption(_ ++ _)

      keysOpt.map { keys =>
        list.foldLeft(Map[K, List[V]]()) { (acc, innerMap) =>
          keys.foldLeft(acc) {
            case (acc, key) =>
              acc + (key -> (acc.getOrElse(key, Nil) :::
                construct(innerMap.get(key), zero.map(z => z(innerMap)))))
          }
        }
      } getOrElse {
        Map()
      }
    }
  }

  /**
   * Implicit class to extend the original scala Future[Option].
   *
   * @param f future to covert
   */
  implicit class ApsoOptionalFuture[A](val f: Future[Option[A]]) extends AnyVal {

    /**
     * If this future returns None or fails, fallback to another future
     *
     * @param other fallback
     * @return resulting future
     */
    def ifNoneOrErrorFallbackTo(other: => Future[Option[A]])(implicit ec: ExecutionContext) = f.flatMap {
      case None =>
        other
      case res =>
        Future.successful(res)
    } recoverWith {
      case _: Exception => other
    }
  }

  /**
   * Implicit class that provides new methods for random number generators.
   * @param rand the `Random` instance to which the new methods are provided.
   */
  final implicit class ApsoRandom(val rand: Random) extends AnyVal {

    /**
     * Chooses a random element from a sequence.
     * @param seq the indexed sequence of elements to choose from
     * @tparam T the type of the elements
     * @return the selected element wrapped in a `Some` if `seq` has at least one element, `None`
     *         otherwise.
     */
    def choose[T](seq: IndexedSeq[T]): Option[T] =
      if (seq.isEmpty) None else Some(seq(rand.nextInt(seq.length)))

    /**
     * Chooses an element of a sequence according to a weight function.
     * @param seq the elements to choose from
     * @param valueFunc the function that maps elements to weights
     * @param r the random value used to select the elements. If the default random value is used,
     *          the weighted selection uses 1.0 as the sum of all weights. To use another scale of
     *          weights, a random value between 0.0 and the maximum weight should be passed.
     * @tparam T the type of the elements
     * @return the selected element wrapped in a `Some` if some element was chosen, `None`
     *         otherwise. Not choosing any element can happen if the weights of the elements do not
     *         sum up to the maximum value of `r`.
     */
    def monteCarlo[T](seq: Traversable[T], valueFunc: T => Double, r: Double): Option[T] =
      if (seq.isEmpty) None
      else {
        val v = valueFunc(seq.head)
        if (r < v) Some(seq.head)
        else monteCarlo(seq.tail, valueFunc, r - v)
      }

    /**
     * Chooses an element of a sequence according to a weight function.
     * @param seq the pairs (element, probability) to choose from
     * @tparam T the type of the elements in the sequence
     * @return the selected element wrapped in a `Some` if some element was chosen, `None`
     *         otherwise. Not choosing any element can happen if the weights of the elements do not
     *         sum up to the maximum value of `r`.
     */
    @inline def monteCarlo[T](seq: Traversable[(T, Double)], r: Double = rand.nextDouble()): Option[T] =
      monteCarlo(seq, { p: (T, Double) => p._2 }, r).map(_._1)

    /**
     * Chooses an element of a sequence according to a weight function.
     * @param seq the sequence of elements to choose from
     * @param valueFunc the function that maps elements to weights
     * @param r the random value used to select the elements. If the default random value is used,
     *          the weighted selection uses 1.0 as the sum of all weights. To use another scale of
     *          weights, a random value between 0.0 and the maximum weight should be passed.
     * @tparam T the type of the elements
     * @return the selected element wrapped in a `Some` if some element was chosen, `None`
     *         otherwise. Not choosing any element can happen if the weights of the elements do not
     *         sum up to the maximum value of `r`.
     */
    @deprecated("Use the equivalent monteCarlo method instead.", "0.4")
    def weightedChoice[T](seq: Seq[T], valueFunc: T => Double, r: Double = rand.nextDouble()) =
      monteCarlo(seq, valueFunc, r)

    /**
     * Chooses a random element of a traversable using the reservoir sampling technique, traversing
     * only once the given sequence.
     * @param seq the elements to choose from
     * @tparam T the type of the elements
     * @return the selected element wrapped in a `Some`, or `None` if the traversable is empty.
     */
    def reservoirSample[T](seq: TraversableOnce[T]): Option[T] =
      seq.foldLeft((None: Option[T], 1)) {
        case ((curr, n), candidate) =>
          (if (rand.nextDouble() < 1.0 / n) Some(candidate) else curr, n + 1)
      }._1

    /**
     * Returns an infinite stream of weighted samples of a sequence.
     * @param seq the elements to choose from
     * @tparam T the type of the elements
     * @return an infinite stream of weighted samples of a sequence.
     */
    def samples[T](seq: Traversable[T], valueFunc: T => Double): Iterator[T] = {
      if (seq.isEmpty) Iterator.empty
      else {
        val len = seq.size
        val scale = len / seq.map(valueFunc).sum
        val scaled = seq.map { e => (e, valueFunc(e) * scale) }.toList
        val (small, large) = scaled.partition(_._2 < 1.0)

        def alias(small: List[(T, Double)],
                  large: List[(T, Double)],
                  rest: List[(T, Double, Option[T])]): List[(T, Double, Option[T])] = {

          (small, large) match {
            case ((s, ps) :: ss, (l, pl) :: ll) =>
              val remainder = (l, pl - (1.0 - ps))
              val newRest = (s, ps, Some(l)) :: rest
              if (remainder._2 < 1)
                alias(remainder :: ss, ll, newRest)
              else
                alias(ss, remainder :: ll, newRest)

            case (_, (l, _) :: ll) => alias(small, ll, (l, 1.0, None) :: rest)
            case ((s, _) :: ss, _) => alias(ss, large, (s, 1.0, None) :: rest)
            case _ => rest
          }
        }

        val table = alias(small, large, Nil).toVector

        def select(p1: Double, p2: Double, table: Vector[(T, Double, Option[T])]): T = {
          table((p1 * len).toInt) match {
            case (a, _, None) => a
            case (a, p, Some(b)) => if (p2 <= p) a else b
          }
        }

        Iterator.continually(select(rand.nextDouble(), rand.nextDouble(), table))
      }
    }

    /**
     * Returns an infinite stream of weighted samples of a sequence.
     * @param seq the pairs (element, probability) to choose from
     * @tparam T the type of the elements
     * @return an infinite stream of weighted samples of a sequence.
     */
    @inline def samples[T](seq: Traversable[(T, Double)]): Iterator[T] =
      samples(seq, { p: (T, Double) => p._2 }).map(_._1)

    /**
     * Returns a decreasingly ordered stream of n doubles in [0, 1], according to a
     * uniform distribution.
     * More Info: BENTLEY, SAXE, Generating Sorted Lists of Random Numbers
     *
     * @param n amount of numbers to generate
     * @return ordered stream of doubles
     */
    def decreasingUniformStream(n: Int): Stream[Double] =
      Stream.iterate((n + 1, 1.0), n + 1) { case (i, currMax) => (i - 1, currMax * math.pow(rand.nextDouble(), 1.0 / i)) }.tail.map(_._2)

    /**
     * Returns an increasingly ordered stream of n doubles in [0, 1], according to a
     * uniform distribution.
     * More Info: BENTLEY, SAXE, Generating Sorted Lists of Random Numbers
     *
     * @param n amount of numbers to generate
     * @return ordered stream of doubles
     */
    def increasingUniformStream(n: Int): Stream[Double] =
      decreasingUniformStream(n).map(1 - _)
  }

  /**
   * Implicit class that provides new methods for closeable resources.
   * @param res the closeable resource to which the new methods are provided.
   */
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
