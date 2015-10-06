package eu.shiftforward.apso.collection

/**
 * Typed map that associates types with values.
 * Based on http://stackoverflow.com/a/7337610/4243494
 */
class TypedMap[T] private (val inner: Map[Manifest[_], T]) {
  def +[U >: T, L <: U](t: Typed[L]) = new TypedMap[U](inner.+[U](t.toPair))
  def +[U >: T, L <: U: Manifest](x: L) = new TypedMap[U](inner.+[U](manifest[L] -> x))
  def -[A <: T: Manifest] = new TypedMap(inner - manifest[A])

  def apply[A <: T: Manifest]: A = inner(manifest[A]).asInstanceOf[A]
  def get[A <: T: Manifest]: Option[A] = inner.get(manifest[A]).map(_.asInstanceOf[A])
  def getOrElse[A <: T: Manifest](default: => A): A = inner.getOrElse(manifest[A], default).asInstanceOf[A]
  def contains[A <: T: Manifest] = inner.contains(manifest[A])
  def size = inner.size
  def values = inner.values
  override def toString = inner.toString()
  override def equals(other: Any) = other match {
    case that: TypedMap[_] => this.inner == that.inner
    case _ => false
  }
  override def hashCode = inner.hashCode()
}

object TypedMap {
  def empty[T] = new TypedMap[T](Map())
  def apply[T](items: Typed[_ <: T]*) = new TypedMap[T](Map(items.map(_.toPair): _*))
}

class Typed[A](value: A)(implicit val key: Manifest[A]) {
  def toPair: (Manifest[_], A) = (key, value)
}
object Typed {
  implicit def toTyped[A: Manifest](a: A) = new Typed(a)
  implicit def toTypable[A](a: A) = new {
    def typedAs[T >: A: Manifest] = new Typed[T](a)(manifest[T])
  }
}