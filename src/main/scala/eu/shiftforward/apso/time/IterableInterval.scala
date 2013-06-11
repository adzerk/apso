package eu.shiftforward.apso.time

import com.github.nscala_time.time.Imports._
import org.joda.time.ReadableInterval

trait IterableInterval extends IndexedSeq[DateTime] {
  val step: Period
  def by(newStep: Period): IterableInterval
}

case class SteppedInterval(interval: ReadableInterval, step: Period)
  extends IterableInterval {

  lazy val length: Int = {
    var i = (interval.toDurationMillis / step.toDurationFrom(interval.getStart).millis).toInt
    if(apply(i) < interval.getEnd) {
      while(apply(i) <= interval.getEnd) { i += 1 }
      i
    } else {
      while(apply(i) > interval.getEnd) { i -= 1 }
      i + 1
    } // FIXME more intelligent code for this?
  }

  def apply(idx: Int) = interval.getStart + step.multipliedBy(idx)

  def by(newStep: Period) = new SteppedInterval(interval, newStep)
}

case class EmptySteppedInterval(step: Period)
  extends IterableInterval {

  def length = 0
  def apply(idx: Int) = throw new IndexOutOfBoundsException
  def by(newStep: Period) = new EmptySteppedInterval(newStep)
}

object IterableInterval {
  def apply(interval: ReadableInterval, step: Period, lastInclusive: Boolean = true): IterableInterval =
    if(lastInclusive) SteppedInterval(interval, step)
    else if(interval.millis == 0) EmptySteppedInterval(step)
    else SteppedInterval(interval.getStart to (interval.getEnd - 1.millis), step)
}
