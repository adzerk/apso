package eu.shiftforward.apso.collection

case class Trie[K, V](value: Option[V] = None, nodes: Map[K, Trie[K, V]] = Map[K, Trie[K, V]]()) {
  def set(k: Seq[K], v: V): Trie[K, V] = {
    k match {
      case Seq(h, t @ _*) =>
        this.copy(value, nodes.get(h) match {
          case Some(trie) =>
            nodes.updated(h, trie.set(t, v))
          case _ =>
            nodes.updated(h, Trie[K, V]().set(t, v))
        })

      case _ =>
        this.copy(Some(v), nodes)
    }
  }

  def get(k: Seq[K]): Option[V] = {
    k match {
      case Seq(h, t @ _*) =>
        nodes.get(h) match {
          case Some(trie) => trie.get(t)
          case _ => None
        }

      case _ =>
        value
    }
  }
}
