package eu.shiftforward.apso.collection

case class MergedBufferedIterator[T](iterators: List[BufferedIterator[T]])(implicit ord: Ordering[T]) extends BufferedIterator[T] {

  def nextIterator = iterators.filter(_.hasNext).minBy(_.head)

  def head = nextIterator.head

  def next() = nextIterator.next()

  def hasNext: Boolean = iterators.exists(_.hasNext)

  /**
   * Lazily merges this buffered iterator with another buffered iterator assuming that both collections
   * are already sorted.
   * @param thatIt the iterator  to merge with this one
   * @tparam U element type of the resulting collection
   * @return the merged iterators
   */
  def mergeSorted[U >: T](thatIt: BufferedIterator[U])(implicit ord: Ordering[U]): BufferedIterator[U] =
    thatIt match {
      case MergedBufferedIterator(thatIts) => MergedBufferedIterator[U](iterators ++ thatIts)
      case _ => MergedBufferedIterator[U](thatIt :: iterators)
    }
}
