package eu.shiftforward.apso.time

import org.joda.time.ReadableInterval
import com.github.nscala_time.time.Imports._

/**
 * Object containing implicit classes and methods related to datetime libraries.
 */
object Implicits {

  /**
   * Implicit class that provides new methods for `LocalDates`.
   * @param d1 the `LocalDate` to which the new methods are provided.
   */
  implicit class ApsoTimeLocalDate(val d1: LocalDate) extends AnyVal {

    /**
     * Returns a `DateTime` corresponding to this `LocalDate` at UTC midnight.
     * @return a `DateTime` corresponding to this `LocalDate` at UTC midnight.
     */
    def utcDateTime: DateTime = d1.toDateTime(new LocalTime(0, 0), DateTimeZone.UTC)

    /**
     * Returns a `DateTime` corresponding to this `LocalDate` at the latest valid
     * time for the date.
     * @return a `DateTime` corresponding to this `LocalDate` at the latest valid
     *         time for the date.
     */
    def toDateTimeAtEndOfDay = (d1 + 1.day).toDateTimeAtStartOfDay - 1.millis
  }

  /**
   * Implicit class that provides new methods for `DateTimes`.
   * @param d1 the `DateTime` to which the new methods are provided.
   */
  final implicit class ApsoTimeDateTime(val d1: DateTime) extends AnyVal {

    /**
     * Returns a `LocalDate` corresponding to this `DateTime` at UTC.
     * @return a `LocalDate` corresponding to this `DateTime` at UTC.
     */
    def utcLocalDate: LocalDate = d1.withZone(DateTimeZone.UTC).toLocalDate

    /**
     * Returns `true` if the given `DateTime` is in the same day as this.
     * @param d2 the second `DateTime`
     * @return `true` if the given `DateTime` is in the same day as this,
     *         `false` othwerwise.
     */
    def isSameDay(d2: DateTime) = d1.year == d2.year && d1.dayOfYear == d2.dayOfYear

    /**
     * Retuns `true` if this `DateTime` is in the range between the two given
     * `DateTimes`.
     * @param dStart the starting `DateTime`
     * @param dEnd the ending `DateTime`
     * @return `true` if this `DateTime` is in the range between the two given
     *         `DateTimes`, `false` otherwise.
     */
    def between(dStart: DateTime, dEnd: DateTime) = isSameDay(dStart) || isSameDay(dEnd) || dStart < d1 && d1 < dEnd

    /**
     * Returns an iterable interval starting at this `DateTime` (inclusive) and
     * ending at the given `DateTime` (exclusive), with a 1 day step.
     * @param d2 the ending `DateTime`
     * @return an iterable interval starting at this `DateTime` (inclusive) and
     *         ending at the given `DateTime` (exclusive), with a 1 day step.
     */
    def until(d2: DateTime) = IterableInterval(d1 to d2, 1.day, false)
  }

  /**
   * Implicit class that provides new methods for `ReadableIntervals`.
   * @param interval the `ReadableInterval` to which the new methods are provided.
   */
  final implicit class ApsoTimeInterval(val interval: ReadableInterval) extends AnyVal {

    /**
     * Partitions this time interval into a given number of equal subintervals.
     * @param n the number of subintervals to return
     * @return a sequence of time intervals resultant of the division of this
     *         interval in `n` equal parts.
     */
    def split(n: Int): Seq[ReadableInterval] = {
      val q = (interval.millis / n).toInt
      (0 until n).map { i => (interval.getStart + q * i) to (interval.getStart + q * (i + 1)) }
    }
  }

  /**
   * Implicit method that allows whe view of a time interval as an indexed
   * sequence. Time intervals are split with a 1 day step.
   * @param interval the time interval to be iterated over
   * @return an iterable time interval.
   */
  implicit def intervalToStepped(interval: ReadableInterval) = IterableInterval(interval, 1.day)
}
