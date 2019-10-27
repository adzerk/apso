package com.velocidi.apso.iterator

/**
 * Object containing implicit classes and methods of general purpose on iterators.
 */
object Implicits {
  /**
   * Implicit class that provides new methods for buffered iterators.
   * @param thisIt the buffered iterator to which the new methods are provided.
   */
  final implicit class ApsoBufferedIterator[T](val thisIt: BufferedIterator[T]) {

    /**
     * Lazily merges this buffered iterator with another buffered iterator assuming that both collections
     * are already sorted.
     * @param thatIt the iterator  to merge with this one
     * @param ord the ordering with which the collections are sorted and with which the merged
     *            collection is to be returned
     * @tparam U element type of the resulting collection
     * @return the merged iterators
     */
    def mergeSorted[U >: T](thatIt: BufferedIterator[U])(implicit ord: Ordering[U]): BufferedIterator[U] =
      MergedBufferedIterator(List(thatIt)).mergeSorted(thisIt)
  }
}
