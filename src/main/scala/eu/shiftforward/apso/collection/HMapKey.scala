package eu.shiftforward.apso.collection

/**
 * A key of an `HMap`.
 * @constructor the default constructor.
 * @tparam V the type of the value associated with this key
 */
class HMapKey[V] extends Serializable {

  /**
   * The type of the value associated with this key. Useful in cases where the
   * type parameter of this key is lost because of type erasure.
   */
  type Value = V

  /**
   * Returns this key, typed as a key associated with values of type `Value`.
   * @return this key, typed as a key associated with values of type `Value`.
   */
  def toKey: HMapKey[Value] = this

  final override def equals(oth: Any) = oth match {
    case obj: AnyRef => this.eq(obj)
    case _ => false
  }
}
