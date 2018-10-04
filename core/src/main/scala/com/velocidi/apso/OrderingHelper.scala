package com.velocidi.apso

/**
 * Object containing utilities related to ordered objects.
 */
@deprecated("Use Ordering[A].min and Ordering[A].max instead", "2017/07/13")
object OrderingHelper {
  def min[A](a: A, b: A)(implicit ord: Ordering[A]): A = ord.min(a, b)
  def max[A](a: A, b: A)(implicit ord: Ordering[A]): A = ord.max(a, b)
}
