package com.kevel.apso

import scala.annotation.tailrec
import scala.collection.Factory
import scala.util.{Random, Try, Using}

/** Object containing implicit classes and methods of general purpose.
  */
object Implicits {

  /** Implicit class that provides new methods for sequences for which their concrete type is important.
    * @param seq
    *   the sequence to which the new methods are provided
    * @tparam T
    *   the type of the elements in the sequence
    * @tparam CC
    *   the concrete type of the sequence
    */
  final implicit class ApsoSeqTyped[T, CC[X] <: Seq[X]](val seq: CC[T]) extends AnyVal {

    /** Merges this sequence with another traversable assuming that both collections are already sorted. This method
      * eagerly evaluates all the elements of both collections, even if they are lazy collections. For that reason,
      * infinite sequences are not supported.
      * @param it
      *   the traversable collection to merge with this one
      * @param bf
      *   combiner factory which provides a combiner
      * @param ord
      *   the ordering with which the collections are sorted and with which the merged collection is to be returned
      * @tparam U
      *   element type of the resulting collection
      * @tparam That
      *   type of the resulting collection
      * @return
      *   this sequence merged with the given traversable
      */
    def mergeSorted[U >: T, That](
        it: IterableOnce[U]
    )(implicit bf: Factory[U, That], ord: Ordering[U]): That = {
      val b = bf.newBuilder

      if (seq.isEmpty) b ++= it
      else {
        val thisIt = seq.iterator
        var thisNext: T = thisIt.next()
        var finished = false

        for (elem <- it.iterator) {

          def takeFromThis(): Unit = {
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

  /** Implicit class that provides new methods for iterable-once collections.
    * @param it
    *   the iterable-once collection to which the new methods are provided.
    */
  final implicit class ApsoIterableOnce[T](val it: IterableOnce[T]) extends AnyVal {

    /** Returns the average of the elements of this collection.
      * @param num
      *   either an instance of `Numeric` or an instance of `Fractional`, defining a set of numeric operations which
      *   includes the `+` and the `/` operators to be used in forming the average.
      * @tparam A
      *   the result type of the `/` operator
      * @return
      *   the average of all elements of this collection with respect to the `+` and `/` operators in `num`.
      */
    def average[A >: T](implicit num: Numeric[A]): A = {
      val div: (A, A) => A = num match {
        case n: Fractional[A] => n.div
        case n: Integral[A]   => n.quot
        case _                => sys.error("Numeric does not support division!")
      }
      val res = it.iterator.foldLeft((num.zero, num.zero)) { (acc, e) =>
        (num.plus(acc._1, e), num.plus(acc._2, num.one))
      }
      if (res._2 == num.zero)
        throw new IllegalArgumentException("The iterable should not be empty!")
      div(res._1, res._2)
    }
  }

  /** Implicit class that provides new methods for maps.
    * @param map
    *   the map to which the new methods are provided.
    */
  final implicit class ApsoMap[A, B](val map: Map[A, B]) extends AnyVal {

    /** Merges a given map with this map. The map is constructed as follows:
      *   - Keys present in one of thw two maps are present in the merged map;
      *   - Keys in both maps are present in the merged map with a value given by `f(thisValue, thatValue)`;
      *
      * @param that
      *   the map to be merged with this map
      * @param f
      *   the function used to merge two values with the same key
      * @return
      *   the merged map.
      */
    def twoWayMerge(that: Map[A, B])(f: (B, B) => B): Map[A, B] =
      map.foldLeft(that) { case (thatMap, (key, mapValue)) =>
        thatMap.get(key) match {
          case Some(thatValue) => thatMap.updated(key, f(mapValue, thatValue))
          case None            => thatMap.updated(key, mapValue)
        }
      }

    /** Applies a given function to all keys of this map. In case `f` is not injective, the behaviour is undefined.
      * @param f
      *   the function to apply to all keys of this map
      * @return
      *   the resulting map with the keys mapped with function `f`.
      */
    def mapKeys[C](f: A => C): Map[C, B] =
      map.map { case (k, v) => f(k) -> v }
  }

  /** Implicit class that provides new methods for random number generators.
    * @param rand
    *   the `Random` instance to which the new methods are provided.
    */
  final implicit class ApsoRandom(val rand: Random) extends AnyVal {

    /** Chooses a random element from an indexed sequence.
      * @param seq
      *   the indexed sequence of elements to choose from
      * @tparam T
      *   the type of the elements
      * @return
      *   the selected element wrapped in a `Some` if `seq` has at least one element, `None` otherwise.
      */
    def choose[T](seq: IndexedSeq[T]): Option[T] =
      if (seq.isEmpty) None else Some(seq(rand.nextInt(seq.length)))

    /** Chooses n random element from a sequence.
      * @param seq
      *   the sequence of elements to choose from
      * @tparam T
      *   the type of the elements
      * @return
      *   the selected elements
      */
    def chooseN[T](seq: Seq[T], n: Int): Seq[T] = {
      @tailrec
      def chooseAux(_seq: Seq[T], _n: Int, acc: Seq[T]): Seq[T] =
        if (_seq.isEmpty || _n <= 0) acc
        else {
          val prob = n.toDouble / _seq.size
          if (rand.nextDouble() < prob) chooseAux(_seq.tail, _n - 1, _seq.head +: acc)
          else chooseAux(_seq.tail, _n, acc)
        }
      chooseAux(seq, n, Seq.empty)
    }

    /** Chooses an element of a sequence according to a weight function.
      * @param seq
      *   the elements to choose from
      * @param valueFunc
      *   the function that maps elements to weights
      * @param r
      *   the random value used to select the elements. If the default random value is used, the weighted selection uses
      *   1.0 as the sum of all weights. To use another scale of weights, a random value between 0.0 and the maximum
      *   weight should be passed.
      * @tparam T
      *   the type of the elements
      * @return
      *   the selected element wrapped in a `Some` if some element was chosen, `None` otherwise. Not choosing any
      *   element can happen if the weights of the elements do not sum up to the maximum value of `r`.
      */
    def monteCarlo[T](seq: Iterable[T], valueFunc: T => Double, r: Double): Option[T] =
      if (seq.isEmpty) None
      else {
        val v = valueFunc(seq.head)
        if (r < v) Some(seq.head)
        else monteCarlo(seq.tail, valueFunc, r - v)
      }

    /** Chooses an element of a sequence according to a weight function.
      * @param seq
      *   the pairs (element, probability) to choose from
      * @tparam T
      *   the type of the elements in the sequence
      * @return
      *   the selected element wrapped in a `Some` if some element was chosen, `None` otherwise. Not choosing any
      *   element can happen if the weights of the elements do not sum up to the maximum value of `r`.
      */
    @inline def monteCarlo[T](seq: Iterable[(T, Double)], r: Double = rand.nextDouble()): Option[T] =
      monteCarlo(seq, { p: (T, Double) => p._2 }, r).map(_._1)

    /** Chooses a random element of a traversable using the reservoir sampling technique, traversing only once the given
      * sequence.
      * @param seq
      *   the elements to choose from
      * @tparam T
      *   the type of the elements
      * @return
      *   the selected element wrapped in a `Some`, or `None` if the traversable is empty.
      */
    def reservoirSample[T](seq: IterableOnce[T]): Option[T] =
      seq.iterator
        .foldLeft((None: Option[T], 1)) { case ((curr, n), candidate) =>
          (if (rand.nextDouble() < 1.0 / n) Some(candidate) else curr, n + 1)
        }
        ._1

    /** Returns an infinite stream of weighted samples of a sequence.
      * @param seq
      *   the elements to choose from
      * @tparam T
      *   the type of the elements
      * @return
      *   an infinite stream of weighted samples of a sequence.
      */
    def samples[T](seq: Iterable[T], valueFunc: T => Double): Iterator[T] = {
      if (seq.isEmpty) Iterator.empty
      else {
        val len = seq.size
        val scale = len / seq.map(valueFunc).sum
        val scaled = seq.map { e => (e, valueFunc(e) * scale) }.toList
        val (small, large) = scaled.partition(_._2 < 1.0)

        def alias(
            small: List[(T, Double)],
            large: List[(T, Double)],
            rest: List[(T, Double, Option[T])]
        ): List[(T, Double, Option[T])] = {

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
            case _                 => rest
          }
        }

        val table = alias(small, large, Nil).toVector

        def select(p1: Double, p2: Double, table: Vector[(T, Double, Option[T])]): T = {
          table((p1 * len).toInt) match {
            case (a, _, None)    => a
            case (a, p, Some(b)) => if (p2 <= p) a else b
          }
        }

        Iterator.continually(select(rand.nextDouble(), rand.nextDouble(), table))
      }
    }

    /** Returns an infinite stream of weighted samples of a sequence.
      * @param seq
      *   the pairs (element, probability) to choose from
      * @tparam T
      *   the type of the elements
      * @return
      *   an infinite stream of weighted samples of a sequence.
      */
    @inline def samples[T](seq: Iterable[(T, Double)]): Iterator[T] =
      samples(seq, { p: (T, Double) => p._2 }).map(_._1)

    /** Returns a decreasingly ordered stream of n doubles in [0, 1], according to a uniform distribution. More Info:
      * BENTLEY, SAXE, Generating Sorted Lists of Random Numbers
      *
      * @param n
      *   amount of numbers to generate
      * @return
      *   ordered stream of doubles
      */
    def decreasingUniformStream(n: Int): LazyList[Double] =
      LazyList
        .iterate((n + 1, 1.0), n + 1) { case (i, currMax) => (i - 1, currMax * math.pow(rand.nextDouble(), 1.0 / i)) }
        .tail
        .map(_._2)

    /** Returns an increasingly ordered stream of n doubles in [0, 1], according to a uniform distribution. More Info:
      * BENTLEY, SAXE, Generating Sorted Lists of Random Numbers
      *
      * @param n
      *   amount of numbers to generate
      * @return
      *   ordered stream of doubles
      */
    def increasingUniformStream(n: Int): LazyList[Double] =
      decreasingUniformStream(n).map(1 - _)
  }

  /** Implicit class that provides new methods for closeable resources.
    * @param res
    *   the closeable resource to which the new methods are provided.
    */
  final implicit class ApsoCloseable[U <: AutoCloseable](val res: U) extends AnyVal {

    /** Uses this resource and closes it afterwards.
      * @param f
      *   the block of code to execute using this resource
      * @tparam T
      *   the return type of the code block.
      * @return
      *   the value returned by the code block.
      */
    def use[T](f: U => T): T = Using(res)(f).get

    /** Uses this resource and closes it afterwards.
      *
      * Any exception thrown by the code block or during the call to `close()` of the `AutoCloseable` resource is caught
      * and presented as a `Failure` in return value.
      *
      * @param f
      *   the block of code to execute using this resource
      * @tparam T
      *   the return type of the code block.
      * @return
      *   a `Try` of the value returned by the code block.
      */
    def tryUse[T](f: U => T): Try[T] = Using(res)(f)
  }
}
