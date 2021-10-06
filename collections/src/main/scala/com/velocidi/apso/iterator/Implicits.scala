package com.velocidi.apso.iterator

/** Object containing implicit classes and methods of general purpose on iterators.
  */
object Implicits {

  /** Implicit conversion from a `BufferedIterator` to a `MergedBufferedIterator`.
    */
  final implicit def toMergeBufferedIterator[T: Ordering](
      it: collection.BufferedIterator[T]
  ): MergedBufferedIterator[T] =
    MergedBufferedIterator(List(it))
}
