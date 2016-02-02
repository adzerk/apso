package eu.shiftforward.apso.collection

import scala.collection.mutable.ListBuffer

/**
 * A map containing keys of heterogeneous values. The keys in this map must
 * implement `HMapKey[V]`, where `V` is the type of its associated key.
 * @param entries a list of key-value pairs present in the map
 * @tparam KeyType the type of keys used in this map
 */
class HMap[KeyType[_] <: HMapKey[_]](val entries: ListBuffer[(KeyType[V], V) forSome { type V }]) {

  /**
   * Retrieves the value which is associated with a given key. If the key does
   * not exist in this map, a `NoSuchElementException` is thrown.
   * @param key the key to lookup
   * @tparam V the type of the value associated with `key`
   * @return the value associated with the given key.
   */
  def apply[V](key: KeyType[V]): V = get(key).getOrElse(throw new NoSuchElementException("Key not found: " + key))

  /**
   * Optionally returns the value associated with a key.
   * @param key the key to lookup
   * @tparam V the type of the value associated with `key`
   * @return an option value containing the value associated with `key` in this
   *         map, or `None` if none exists.
   */
  def get[V](key: KeyType[V]): Option[V] = entries.collectFirst { case (k, v) if k == key => v.asInstanceOf[V] }

  /**
   * Returns the value associated with a key, or a default value if the key is
   * not contained in the map.
   * @param key the key to lookup
   * @param default a computation that yields a default value in case no binding
   *        for `key` is found in the map
   * @tparam V the type of the value associated with `key`
   * @return the value associated with `key` if it exists, otherwise the result
   *         of the `default` computation.
   */
  def getOrElse[V](key: KeyType[V], default: => V): V = get(key).getOrElse(default)

  /**
   * If given key is already in this map, returns associated value. Otherwise,
   * computes value from given expression `op`, stores with key in map and
   * returns that value.
   * @param key the key to lookup and possibly update
   * @param op the computation yielding the value to associate with `key`, if
   *        `key` is previously unbound
   * @tparam V the type of the value associated with `key`
   * @return the value associated with key (either previously or as a result of
   *         executing the method).
   */
  def getOrElseUpdate[V](key: KeyType[V], op: => V): V = get(key) match {
    case Some(k) => k
    case None => val newVal = op; put(key, newVal); newVal
  }

  /**
   * Adds a new key/value pair to this map. If the map already contains a
   * mapping for the key, it will be overridden by the new value.
   * @param key the key to update
   * @param value the new value
   * @tparam V the type of the value associated with `key`
   * @return this map.
   */
  def put[V](key: KeyType[V], value: V): this.type = { (key -> value) +=: entries; this }

  /**
   * Adds a new key/value pair to this map. If the map already contains a
   * mapping for the key, it will be overridden by the new value.
   * @param entry the key-value pair to update
   * @tparam V the type of the value in `entry`
   * @return this map.
   */
  def +=[V](entry: (KeyType[V], V)): this.type = { put(entry._1, entry._2) }

  /**
   * Creates a new map containing the key/value mappings provided by the
   * specified `HMap` and all the key/value mappings of this map.
   * @param other the map to append
   * @return a new map containing mappings of this map and those provided by
   *         `other`.
   */
  def ++(other: HMap[KeyType]): HMap[KeyType] = {
    val newCase = HMap(other.entries.toList: _*)
    entries.foreach { case (k, v) => newCase.getOrElseUpdate(k, v) }
    newCase
  }

  /**
   * Builds a new collection by applying a function to all entries of this map.
   * @param f the function to apply to each element
   * @tparam A the element type of the returned collection
   * @return a new collection resulting from applying the given function `f` to
   *         each element of this map and collecting the results.
   */
  def map[A](f: ((KeyType[V], V) forSome { type V }) => A) = entries.map(f)

  /**
   * Applies a function `f` to all elements of this map.
   * @param f the function that is applied for its side-effect to every element.
   *        The result of function `f` is discarded.
   */
  def foreach(f: ((KeyType[V], V) forSome { type V }) => Unit) { entries.foreach(f) }

  /**
   * Returns a copy of this map.
   * @return a copy of this map.
   */
  def copy: HMap[KeyType] = HMap() ++ this

  override def toString = entries.map { case (k, v) => s"$k -> $v" }.mkString("HMap(", ", ", ")")

  override def equals(oth: Any) = oth match {
    case obj: HMap[_] => entries.toSet == obj.entries.toSet
    case _ => false
  }

  override def hashCode() = entries.toSet.hashCode()
}

/**
 * Object containing factory methods for `HMaps`.
 */
object HMap {

  /**
   * Creates an `HMap` with the given entries.
   * @param entries the entries to be present in the new map
   * @tparam KeyType the type of keys used in this map
   * @return a new map containing the given entries.
   */
  def apply[KeyType[_] <: HMapKey[_]](entries: (KeyType[V], V) forSome { type V }*): HMap[KeyType] =
    new HMap(ListBuffer(entries: _*))
}
