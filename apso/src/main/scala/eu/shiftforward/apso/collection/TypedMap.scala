package eu.shiftforward.apso.collection

/**
 * Typed map that associates types with values.
 * Based on http://stackoverflow.com/a/7337610/4243494
 */
class TypedMap private (val inner: Map[Manifest[_], Any]) {
  def +[A](t: Typed[A]) = new TypedMap(inner + t.toPair)
  def +[A: Manifest](a: A) = new TypedMap(inner + (manifest[A] -> a))
  def -[A: Manifest] = new TypedMap(inner - manifest[A])
  def apply[A: Manifest]: A = inner(manifest[A]).asInstanceOf[A]
  def get[A: Manifest]: Option[A] = inner.get(manifest[A]).map(_.asInstanceOf[A])
  def getOrElse[A: Manifest](default: => A): A = inner.getOrElse(manifest[A], default).asInstanceOf[A]
  def contains[A: Manifest] = inner.contains(manifest[A])
  def size = inner.size
  override def toString = inner.toString()
  override def equals(other: Any) = other match {
    case that: TypedMap => this.inner == that.inner
    case _ => false
  }
  override def hashCode = inner.hashCode()
}

object TypedMap {
  val empty = new TypedMap(Map())
  def apply(items: Typed[_]*) = new TypedMap(Map(items.map(_.toPair): _*))
}

class Typed[A](value: A)(implicit val key: Manifest[A]) {
  def toPair: (Manifest[_], Any) = (key, value)
}
object Typed {
  implicit def toTyped[A: Manifest](a: A) = new Typed(a)
  implicit def toTypable[A](a: A) = new {
    def typedAs[T >: A: Manifest] = new Typed[T](a)(manifest[T])
  }
}