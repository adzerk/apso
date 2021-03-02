package com.velocidi.apso.time

import com.github.nscala_time.time.Imports._
import org.specs2.mutable._

import Implicits._

class ImplicitsSpec extends Specification {
  "An ApsoTimeLocalDate" should {
    "support range creation" in {
      val startDate = "2014-01-01".toLocalDate
      val endDate = "2014-01-03".toLocalDate

      (startDate to endDate) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-02".toLocalDate, "2014-01-03".toLocalDate)

      (startDate until endDate) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-02".toLocalDate)

      (startDate until "2014-01-06".toLocalDate by 2.days) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-03".toLocalDate, "2014-01-05".toLocalDate)
    }

    "support conversion to UTC DateTime" in {
      val utcDateTime = "2014-01-01".toLocalDate.utcDateTime

      utcDateTime === new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    }

    "support conversion to DateTime at the end of day" in {
      val dateTime = "2014-01-01".toLocalDate.toDateTimeAtEndOfDay

      dateTime === new DateTime(2014, 1, 1, 23, 59, 59, 999)
    }

    "support conversion to DateTime at the end of day (with target timezone)" in {
      val dateTime = "2014-01-01".toLocalDate.toDateTimeAtEndOfDay(DateTimeZone.forID("EST"))

      dateTime === new DateTime(2014, 1, 1, 23, 59, 59, 999, DateTimeZone.forID("EST"))
    }
  }

  "An ApsoTimeDateTime" should {

    "support conversion to UTC LocalDate" in {
      val localDate = "2014-01-01".toLocalDate
      val dateTime = localDate.toDateTimeAtStartOfDay.toDateTime(DateTimeZone.forID("EST"))
      val estLocalDate = dateTime.toLocalDate
      val utcLocalDate = dateTime.utcLocalDate

      estLocalDate === new LocalDate(2013, 12, 31)
      utcLocalDate === new LocalDate(2014, 1, 1)
    }

    "support same day comparison" in {
      val dateTimeStart = "2014-01-01".toLocalDate.toDateTimeAtStartOfDay
      val dateTimeEnd = "2014-01-01".toLocalDate.toDateTimeAtEndOfDay
      val dateTimeOther = "2014-01-02".toLocalDate.toDateTimeAtStartOfDay

      dateTimeStart.isSameDay(dateTimeEnd) must beTrue
      dateTimeOther.isSameDay(dateTimeEnd) must beFalse
      dateTimeOther.isSameDay(dateTimeStart) must beFalse
    }

    "be able to check if a date time is between two date times" in {
      val dateTimeStart = "2014-01-01".toLocalDate.toDateTimeAtStartOfDay
      val dateTimeEnd = "2014-01-01".toLocalDate.toDateTimeAtEndOfDay
      val dateTimeOther = "2014-01-02".toLocalDate.toDateTimeAtStartOfDay

      dateTimeStart.between(dateTimeEnd, dateTimeOther) must beFalse
      dateTimeEnd.between(dateTimeStart, dateTimeOther) must beTrue
      dateTimeStart.between(dateTimeOther, dateTimeEnd) must beFalse
    }

    "support iterable interval creation" in {
      val dateTime1Start = "2014-01-01".toLocalDate.toDateTimeAtStartOfDay
      val dateTime2Start = "2014-01-02".toLocalDate.toDateTimeAtStartOfDay
      val dateTime3Start = "2014-01-03".toLocalDate.toDateTimeAtStartOfDay
      val dateTime3End = "2014-01-03".toLocalDate.toDateTimeAtEndOfDay

      dateTime1Start.until(dateTime3Start).toList === List(dateTime1Start, dateTime2Start)
      dateTime1Start.until(dateTime3End).toList === List(dateTime1Start, dateTime2Start, dateTime3Start)
      dateTime3End.until(dateTime1Start) must throwAn[IllegalArgumentException]
    }
  }

  "An ApsoTimeInterval" should {
    "split itself into subsequences" in {
      val interval = new Interval(0, 99)
      interval.split(0) === Seq.empty
      interval.split(-1) must throwAn[IllegalArgumentException]
      interval.split(5).size === 5
      interval.split(4) === Seq(
        new Interval(0, 24),
        new Interval(25, 49),
        new Interval(50, 74),
        new Interval(75, 99))
    }
  }
}
