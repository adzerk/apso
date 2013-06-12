package eu.shiftforward.apso.collection

class HMapKey[V] {
  type Value = V
  def toKey: HMapKey[Value] = this

  final override def equals(oth: Any) = oth match {
    case obj: AnyRef => this.eq(obj)
    case _ => false
  }
}
