package eu.shiftforward.apso

/**
  * Object containing utilities related to ordered objects.
  */
object OrderingHelper {
  def min[A](a: A, b: A)(implicit ord: Ordering[A]): A = ord.min(a, b)
  def max[A](a: A, b: A)(implicit ord: Ordering[A]): A = ord.max(a, b)
}
