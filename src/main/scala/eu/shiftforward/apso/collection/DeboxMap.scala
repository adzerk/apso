package eu.shiftforward.apso.collection

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.{ specialized => spec }

/**
 * Exception thrown when a `DeboxMap` factory is called with a different number
 * of keys and values.
 * @param k the number of keys
 * @param v the number of values
 */
class InvalidSizes(k: Int, v: Int) extends Exception("%s, %s" format (k, v))

/**
 * Exception thrown when a `DeboxMap` factory is called requiring an invalid
 * preallocated size.
 * @param n the requested size
 */
class MapOverflow(n: Int) extends Exception("size %s exceeds max" format n)

/**
 * Exception thrown when trying to access a non-existent key in a `DeboxMap`.
 * @param k the non-existent key
 */
class NotFound(k: String) extends Exception("key %s was not found" format k)

/**
 * Object containing factory methods for `DeboxMaps`.
 */
object DeboxMap {

  /**
   * Creates an empty `DeboxMap`.
   * @tparam A the type of the keys of the new map
   * @tparam B the type of the values of the new map
   * @return a new, empty `DeboxMap`.
   */
  def empty[@spec(Int, Long, Double, AnyRef) A: ClassTag, @spec(Int, Long, Double, AnyRef) B: ClassTag] =
    new DeboxMap(new Array[A](8), new Array[B](8), new Array[Byte](8), 0, 0)

  /**
   * Creates a DeboxMap preallocated to a particular size. Note that the
   * internal representation may allocate more space than requested to satisfy
   * the requirements of internal alignment. DeboxMap uses arrays whose lengths
   * are powers of two.
   * @param n the minimum number of elements to allocate space for
   * @tparam A the type of the keys of the new map
   * @tparam B the type of the values of the new map
   * @return a new `DeboxMap` with the required preallocated space.
   */
  def ofDim[@spec(Int, Long, Double, AnyRef) A: ClassTag, @spec(Int, Long, Double, AnyRef) B: ClassTag](n: Int) = {
    val sz = nextPowerOfTwo(n)
    if (sz < 1) throw new MapOverflow(n)
    new DeboxMap(new Array[A](sz), new Array[B](sz), new Array[Byte](sz), 0, 0)
  }

  /**
   * Creates an empty DeboxMap.
   * @tparam A the type of the keys of the new map
   * @tparam B the type of the values of the new map
   * @return a new, empty `DeboxMap`.
   */
  def apply[@spec(Int, Long, Double, AnyRef) A: ClassTag, @spec(Int, Long, Double, AnyRef) B: ClassTag]() = empty[A, B]

  /**
   * Creates a map from an array of keys and another array of values.
   * @param ks the array of keys to add to the map
   * @param vs the array of corresponding values to add to the map
   * @tparam A the type of the keys of the new map
   * @tparam B the type of the values of the new map
   * @return a new `DeboxMap` with the given elements
   */
  def apply[@spec(Int, Long, Double, AnyRef) A: ClassTag, @spec(Int, Long, Double, AnyRef) B: ClassTag](
    ks: Array[A],
    vs: Array[B]) = {

    if (ks.length != vs.length) throw new InvalidSizes(ks.length, vs.length)
    val map = ofDim[A, B](ks.length)
    val limit = ks.length - 1
    @inline
    @tailrec def loop(i: Int) {
      map(ks(i)) = vs(i)
      if (i < limit) loop(i + 1)
    }
    loop(0)
    map
  }

  private[this] def nextPowerOfTwo(n: Int): Int = {
    val x = java.lang.Integer.highestOneBit(n)
    if (x == n) n else x * 2
  }
}

/**
 * An hash map optimized for performance, not incurring in boxing while storing
 * primitive values.
 * @tparam A the type of the keys
 * @tparam B the type of the values
 */
final class DeboxMap[@spec(Int, Long, Double, AnyRef) A: ClassTag, @spec(Int, Long, Double, AnyRef) B: ClassTag] protected[apso] (
  ks: Array[A],
  vs: Array[B],
  bs: Array[Byte],
  n: Int,
  u: Int)
    extends (A => B) with Serializable {

  // set internals
  var keys: Array[A] = ks
  // keys track set/unset
  var vals: Array[B] = vs
  // values mirror keys
  var buckets: Array[Byte] = bs
  // buckets track defined/used
  var len: Int = n
  // number of defined items in map
  var used: Int = u // number of buckets used in map

  def getBuckets: Array[Byte] = buckets

  // hashing internals
  var mask = keys.length - 1
  // size - 1, used for hashing
  var limit = (keys.length * 0.65).toInt // point at which we should resize

  /**
   * Returns the number of entries in this map.
   * @return the number of entries in this map.
   */
  final def length: Int = len

  /**
   * Updates the value of a key. If the key does not exist, it is added to the
   * map.
   * @param key the key to update
   * @param value the value to assign to the given key
   */
  final def update(key: A, value: B) {
    @inline
    @tailrec def loop(i: Int, perturbation: Int) {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        keys(j) = key
        vals(j) = value
        buckets(j) = 3
        len += 1
        used += 1
        if (used > limit) resize()
      }
      else if (status == 2) {
        keys(j) = key
        vals(j) = value
        buckets(j) = 3
        len += 1
      }
      else if (keys(j) == key) {
        vals(j) = value
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Removes a key from this map. If the key does not exist, this method does
   * nothing.
   * @param key the key to remove
   */
  final def remove(key: A) {
    @inline
    @tailrec def loop(i: Int, perturbation: Int) {
      val j = i & mask
      val status = buckets(j)
      if (status == 3 && keys(j) == key) {
        buckets(j) = 2
        len -= 1
      }
      else if (status == 0) {
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Returns a copy of this map.
   * @return a copy of this map.
   */
  final def copy: DeboxMap[A, B] =
    new DeboxMap(keys.clone(), vals.clone(), buckets.clone(), len, used)

  /**
   * Tests if this map contains a given key.
   * @param key the key to test for membership
   * @return `true` if the map contains the given key, `false` otherwise.
   */
  final def contains(key: A): Boolean = {
    @inline
    @tailrec def loop(i: Int, perturbation: Int): Boolean = {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        false
      }
      else if (status == 3 && keys(j) == key) {
        true
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Returns the value associated with a key. If the key does not exist, a
   * `NotFound` exception is thrown.
   * @param key the key to lookup
   * @return the value associated with the given key.
   */
  final def apply(key: A): B = {
    @inline
    @tailrec def loop(i: Int, perturbation: Int): B = {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        throw new NotFound(key.toString)
      }
      else if (status == 3 && keys(j) == key) {
        vals(j)
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Returns the value associated with a key or `None` if the key does not
   * exist.
   * @param key the key to lookup
   * @return the value associated with the given key wrapped in a `Some` if this
   * map contains the key, `None` otherwise.
   */
  final def get(key: A): Option[B] = {
    @inline
    @tailrec def loop(i: Int, perturbation: Int): Option[B] = {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        None
      }
      else if (status == 3 && keys(j) == key) {
        Some(vals(j))
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Returns the value associated with a key or a default value if the key does
   * not exist.
   * @param key the key to lookup
   * @param default the value to return if this map does not contain the given
   * key
   * @return the value associated with the given key if this map contains the
   * key, `default` otherwise.
   */
  final def getOrElse(key: A, default: B): B = {
    @inline
    @tailrec def loop(i: Int, perturbation: Int): B = {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        default
      }
      else if (status == 3 && keys(j) == key) {
        vals(j)
      }
      else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = key.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Applies a function `f` to all entries of this map.
   * @param f the function that is applied for its side-effect to every element.
   * The result of function `f` is discarded.
   */
  final def foreach(f: (A, B) => Unit) {
    @inline
    @tailrec
    def loop(i: Int, count: Int, limit: Int) {
      val c = if (buckets(i) == 3) {
        f(keys(i), vals(i))
        count + 1
      }
      else {
        count
      }
      if (c <= limit) loop(i + 1, c, limit)
    }
    loop(0, 0, length - 1)
  }

  /**
   * Builds a new list by applying a function to all entries of this map.
   * @param f the function to apply to each element
   * @tparam C the type of the elements in the returned list
   * @return a new list resulting from applying the given function `f` to each
   * element of this map and collecting the results.
   */
  final def map[C](f: (A, B) => C): List[C] = {
    @inline
    @tailrec
    def loop(i: Int, count: Int, limit: Int, acc: List[C]): List[C] = {
      if (buckets(i) == 3) {
        val newAcc = f(keys(i), vals(i)) :: acc
        val c = count + 1
        if (c <= limit) {
          loop(i + 1, c, limit, newAcc)
        }
        else {
          newAcc
        }
      }
      else if (count <= limit) {
        loop(i + 1, count, limit, acc)
      }
      else {
        acc
      }
    }
    loop(0, 0, length - 1, Nil)
  }

  final override def equals(that: Any) = {
    def equalsInner[A, B](m1: DeboxMap[A, B], m2: DeboxMap[A, B]) = {
      var equals = true
      m1.foreach {
        case (k, v) =>
          equals &= m2.contains(k) && m2(k) == v
      }
      equals
    }

    that match {
      case that: DeboxMap[A, B] => equalsInner(this, that) && equalsInner(that, this)
      case _ => false
    }
  }

  final def hash(item: A, _mask: Int, _keys: Array[A], _buckets: Array[Byte]): Int = {
    @inline
    @tailrec
    def loop(i: Int, perturbation: Int): Int = {
      val j = i & _mask
      if (_buckets(j) == 3 && _keys(j) != item)
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      else
        j
    }
    val i = item.## & 0x7fffffff
    loop(i, i)
  }

  final def resize() {
    val size = keys.length
    val factor = if (size < 10000) 4 else 2

    val nextsize = size * factor
    val nextmask = nextsize - 1
    val nextkeys = new Array[A](nextsize)
    val nextvals = new Array[B](nextsize)
    val nextbs = new Array[Byte](nextsize)

    @inline
    @tailrec
    def loop(i: Int, limit: Int) {
      if (buckets(i) == 3) {
        val item = keys(i)
        val j = hash(item, nextmask, nextkeys, nextbs)
        nextkeys(j) = item
        nextvals(j) = vals(i)
        nextbs(j) = 3
      }
      if (i < limit) loop(i + 1, limit)
    }
    loop(0, keys.length - 1)

    keys = nextkeys
    vals = nextvals
    buckets = nextbs
    mask = nextmask
    limit *= factor
  }
}
