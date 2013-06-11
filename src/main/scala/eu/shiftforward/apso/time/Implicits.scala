package eu.shiftforward.apso.time

import com.github.nscala_time.time.Imports._
import org.joda.time.ReadableInterval

object Implicits {

  final implicit class RichDateTime(val d1: DateTime) extends AnyVal {
    def utcLocalDate: LocalDate = d1.withZone(DateTimeZone.UTC).toLocalDate

    def isSameDay(d2: DateTime) = d1.year == d2.year && d1.dayOfYear == d2.dayOfYear
    def between(dStart: DateTime, dEnd: DateTime) = isSameDay(dStart) || isSameDay(dEnd) || dStart < d1 && d1 < dEnd

    def until(d2: DateTime) = IterableInterval(d1 to d2, 1.day, false)
  }

  implicit class RichInterval(val interval: ReadableInterval) extends AnyVal {
    def split(n: Int): Seq[ReadableInterval] = {
      val q = (interval.millis / n).toInt
      (0 until n).map { i => (interval.getStart + q * i) to (interval.getStart + q * (i + 1)) }
    }
  }

  implicit def intervalToStepped(interval: ReadableInterval) = IterableInterval(interval, 1.day)
}