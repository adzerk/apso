package com.velocidi.apso.iterator

import scala.collection.BufferedIterator

/** Object containing implicit classes and methods of general purpose on iterators.
  */
object Implicits {

  /** Implicit conversion from a `BufferedIterator` to a `MergedBufferedIterator`.
    */
  final implicit def toMergeBufferedIterator[T: Ordering](
      it: BufferedIterator[T]
  ): MergedBufferedIterator[T] =
    MergedBufferedIterator(List(it))
}
