package com.kevel.apso.time

import org.joda.time.{DateTime, Interval, LocalDate, Period, ReadableInstant, ReadableInterval}

/** A view of a time interval as an indexed sequence of `DateTimes`.
  */
trait IterableInterval extends IndexedSeq[DateTime] {

  /** The period of time between consecutive `DateTimes`.
    */
  val step: Period

  /** Returns an iterable interval with the same time range and with the given step.
    * @param step
    *   the step of the interval to return
    * @return
    *   an iterable interval with the same time range and with the given step.
    */
  def by(step: Period): IterableInterval
}

/** A view of a `ReadableInterval` as an indexed sequence of `DateTimes`.
  * @param interval
  *   the `ReadableInterval` to view as an indexed sequence
  * @param step
  *   the period of time between consecutive `DateTimes` in the sequence
  */
case class SteppedInterval(interval: ReadableInterval, step: Period) extends IterableInterval {

  lazy val length: Int = {
    var i = (interval.toDurationMillis / step.toDurationFrom(interval.getStart).getMillis).toInt
    if (apply(i).isBefore(interval.getEnd)) {
      while (apply(i).isBefore(interval.getEnd)) { i += 1 }
      i
    } else {
      while (!apply(i).isBefore(interval.getEnd)) { i -= 1 }
      i + 1
    } // FIXME more intelligent code for this?
  }

  def apply(idx: Int) = interval.getStart.plus(step.multipliedBy(idx))

  def by(newStep: Period) = new SteppedInterval(interval, newStep)
}

/** An iterable time interval with no elements.
  * @param step
  *   the period of time between consecutive `DateTimes`. Does not affect an empty interval
  */
case class EmptySteppedInterval(step: Period) extends IterableInterval {

  def length = 0
  def apply(idx: Int) = throw new IndexOutOfBoundsException
  def by(newStep: Period) = new EmptySteppedInterval(newStep)
}

/** Companion object containing a facotry for iterable time intervals.
  */
object IterableInterval {

  /** Creates a new iterable time interval from a `ReadableInterval`.
    * @param interval
    *   the `ReadableInterval` to view as an indexed sequence
    * @param step
    *   the period of time between consecutive `DateTimes`
    * @return
    *   an iterable time interval with the given step.
    */
  def apply(interval: ReadableInterval, step: Period): IterableInterval =
    if (interval.toDuration.getMillis == 0) EmptySteppedInterval(step)
    else SteppedInterval(interval, step)

  /** Creates a new iterable time interval from `start` (inclusive) to `end` (exclusive).
    * @param start
    *   the first `ReadableInstant` of the interval, the first element of the indexed sequence
    * @param end
    *   the last exclusive `ReadableInstant` of the interval, the first element not in the sequence
    * @param step
    *   the period of time between consecutive `DateTimes`
    * @return
    *   an iterable time interval with the given step.
    */
  def apply(start: ReadableInstant, end: ReadableInstant, step: Period): IterableInterval =
    IterableInterval(new Interval(start, end), step)
}

/** A view of a time interval as an indexed sequence of `LocalDate`.
  */
case class LocalDateInterval(i: IterableInterval) extends IndexedSeq[LocalDate] {
  def apply(idx: Int): LocalDate = i(idx).toLocalDate
  def by(newStep: Period) = LocalDateInterval(i.by(newStep))
  def length = i.length
}
