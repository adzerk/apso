package com.velocidi.apso.collection

import scala.reflect.ClassTag

/**
 * Typed map that associates types with values.
 * Based on http://stackoverflow.com/a/7337610/4243494
 */
class TypedMap[T] private (val inner: Map[ClassTag[_], T]) {
  def +[U >: T, L <: U](t: Typed[L]) = new TypedMap[U](inner.+[U](t.toPair))
  def +[U >: T, L <: U: ClassTag](x: L)(implicit ct: ClassTag[L]) = new TypedMap[U](inner.+[U](ct -> x))
  def -[A <: T: ClassTag](implicit ct: ClassTag[A]) = new TypedMap(inner - ct)

  def apply[A <: T: ClassTag](implicit ct: ClassTag[A]): A = inner(ct).asInstanceOf[A]
  def get[A <: T: Manifest](implicit ct: ClassTag[A]): Option[A] = inner.get(ct).map(_.asInstanceOf[A])
  def getOrElse[A <: T: ClassTag](default: => A)(implicit ct: ClassTag[A]): A = inner.getOrElse(ct, default).asInstanceOf[A]
  def contains[A <: T: ClassTag](implicit ct: ClassTag[A]) = inner.contains(ct)
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

class Typed[A](value: A)(implicit val key: ClassTag[A]) {
  def toPair: (ClassTag[_], A) = (key, value)
}
object Typed {
  implicit def toTyped[A: ClassTag](a: A) = new Typed(a)
  implicit def toTypable[A](a: A) = new {
    def typedAs[T >: A: ClassTag](implicit ct: ClassTag[T]) = new Typed[T](a)(ct)
  }
}
