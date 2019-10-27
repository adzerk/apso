package com.velocidi.apso.iterator

import scala.collection.mutable.PriorityQueue

case class MergedBufferedIterator[T](iterators: List[BufferedIterator[T]])(implicit ord: Ordering[T]) extends BufferedIterator[T] {
  private[this] implicit def bufferedIteratorOrdering = new Ordering[BufferedIterator[T]] {
    def compare(i1: BufferedIterator[T], i2: BufferedIterator[T]) =
      ord.compare(i2.head, i1.head)
  }

  private[this] lazy val nonEmptyIterators = {
    val pq = new PriorityQueue[BufferedIterator[T]]
    pq.enqueue(iterators.filter(_.hasNext): _*)
    pq
  }

  def head = nonEmptyIterators.head.head

  def next() = {
    val topIt = nonEmptyIterators.dequeue()
    val res = topIt.next()
    if (topIt.hasNext)
      nonEmptyIterators.enqueue(topIt)
    res
  }

  def hasNext: Boolean = nonEmptyIterators.nonEmpty

  /**
   * Lazily merges this buffered iterator with another buffered iterator assuming that both collections
   * are already sorted.
   * @param thatIt the iterator  to merge with this one
   * @tparam U element type of the resulting collection
   * @return the merged iterators
   */
  def mergeSorted[U >: T](thatIt: BufferedIterator[U])(implicit ord: Ordering[U]): BufferedIterator[U] =
    thatIt match {
      case mIt @ MergedBufferedIterator(thatIts) => MergedBufferedIterator[U](nonEmptyIterators.toList ++ thatIts)
      case _ => MergedBufferedIterator[U](thatIt :: nonEmptyIterators.toList)
    }
}
