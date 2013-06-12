package eu.shiftforward.apso.collection

import collection.mutable.ListBuffer

class HMap[KeyType[_] <: HMapKey[_]](val entries: ListBuffer[(KeyType[V], V) forSome { type V }]) {

  def apply[V](key: KeyType[V]): V = get(key).getOrElse(throw new NoSuchElementException("Key not found: " + key))
  def get[V](key: KeyType[V]): Option[V] = entries.collectFirst { case (k, v) if k == key => v.asInstanceOf[V] }
  def getOrElse[V](key: KeyType[V], default: V): V = get(key).getOrElse(default)
  def getOrElseUpdate[V](key: KeyType[V], default: V): V = get(key) match {
    case Some(k) => k
    case None => put(key, default); default
  }

  def put[V](key: KeyType[V], value: V): this.type = { (key -> value) +=: entries; this }
  def +=[V](entry: (KeyType[V], V)) { put(entry._1, entry._2) }
  def ++(otherCase: HMap[KeyType]): HMap[KeyType] = {
    val newCase = HMap(otherCase.entries.toList: _*)
    entries.foreach { case (k, v) => newCase.getOrElseUpdate(k, v) }
    newCase
  }

  def map[A](func: ((KeyType[V], V) forSome { type V }) => A) = entries.map(func)
  def foreach(func: ((KeyType[V], V) forSome { type V }) => Unit) { entries.foreach(func) }

  def copy: HMap[KeyType] = HMap() ++ this

  override def toString = entries.mkString("HMap(", ", ", ")")
  override def equals(oth: Any) = oth match {
    case obj: HMap[_] => entries == obj.entries
    case _ => false
  }
}

object HMap {
  def apply[KeyType[_] <: HMapKey[_]](entries: (KeyType[V], V) forSome { type V }*): HMap[KeyType] =
    new HMap(ListBuffer(entries: _*))
}
