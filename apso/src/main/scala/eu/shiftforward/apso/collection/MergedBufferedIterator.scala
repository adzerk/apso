package eu.shiftforward.apso.collection

case class MergedBufferedIterator[T](iterators: List[BufferedIterator[T]])(implicit ord: Ordering[T]) extends BufferedIterator[T] {

  var nonEmptyIterators = iterators.filter(_.hasNext)

  def nextIterator = nonEmptyIterators.minBy(_.head)

  def head = nextIterator.head

  def next() = {
    val res = nextIterator.next()
    nonEmptyIterators = iterators.filter(_.hasNext)
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
      case mIt @ MergedBufferedIterator(thatIts) => MergedBufferedIterator[U](nonEmptyIterators ++ thatIts)
      case _ => MergedBufferedIterator[U](thatIt :: nonEmptyIterators)
    }
}
