package com.kevel.apso.collection

/** An immutable implementation of a Trie (https://en.wikipedia.org/wiki/Trie).
  *
  * @param value
  *   the optional value of this node
  * @param nodes
  *   the descendants of this node
  *
  * @tparam K
  *   the type param of keys in the nodes
  * @tparam V
  *   the type param of values stored in the trie
  */
case class Trie[K, V](value: Option[V] = None, nodes: Map[K, Trie[K, V]] = Map[K, Trie[K, V]]()) {

  /** Sets the provided value `v` for the key `k`.
    */
  def set(k: Seq[K], v: V): Trie[K, V] = {
    k match {
      case Seq(h, t @ _*) =>
        this.copy(
          value,
          nodes.get(h) match {
            case Some(trie) =>
              nodes.updated(h, trie.set(t, v))
            case _ =>
              nodes.updated(h, Trie[K, V]().set(t, v))
          }
        )

      case _ =>
        this.copy(Some(v), nodes)
    }
  }

  /** Gets the value for the key `k`.
    */
  def get(k: Seq[K]): Option[V] = {
    k match {
      case Seq(h, t @ _*) =>
        nodes.get(h) match {
          case Some(trie) => trie.get(t)
          case _          => None
        }

      case _ =>
        value
    }
  }
}
