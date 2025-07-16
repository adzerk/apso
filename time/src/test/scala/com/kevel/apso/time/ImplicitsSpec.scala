package com.kevel.apso.time

import org.joda.time.{DateTime, DateTimeZone, Interval, LocalDate, LocalDateTime, Period}
import org.specs2.mutable._

import Implicits._

class ImplicitsSpec extends Specification {
  "An ApsoTimeLocalDate" should {
    "support range creation" in {
      val startDate = new LocalDate("2014-01-01")
      val endDate = new LocalDate("2014-01-03")

      (startDate to startDate) === IndexedSeq(startDate)

      (startDate to endDate) === IndexedSeq(
        new LocalDate("2014-01-01"),
        new LocalDate("2014-01-02"),
        new LocalDate("2014-01-03")
      )

      (startDate until startDate) === IndexedSeq.empty

      (startDate until endDate) === IndexedSeq(new LocalDate("2014-01-01"), new LocalDate("2014-01-02"))

      (startDate until new LocalDate("2014-01-06") by Period.days(2)) === IndexedSeq(
        new LocalDate("2014-01-01"),
        new LocalDate("2014-01-03"),
        new LocalDate("2014-01-05")
      )
    }

    "support conversion to UTC DateTime" in {
      val utcDateTime = new LocalDate("2014-01-01").utcDateTime

      utcDateTime === new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    }

    "support conversion to DateTime at the end of day" in {
      val dateTime = new LocalDate("2014-01-01").toDateTimeAtEndOfDay

      dateTime === new DateTime(2014, 1, 1, 23, 59, 59, 999)
    }

    "support conversion to DateTime at the end of day (with target timezone)" in {
      val dateTime = new LocalDate("2014-01-01").toDateTimeAtEndOfDay(DateTimeZone.forID("EST"))

      dateTime === new DateTime(2014, 1, 1, 23, 59, 59, 999, DateTimeZone.forID("EST"))
    }
  }

  "An ApsoTimeDateTime" should {

    "support conversion to UTC LocalDateTime" in {
      val localDateTime = LocalDateTime.parse("2014-01-01T01:10:58")
      val dateTime = localDateTime.toDateTime(DateTimeZone.forID("NZ"))
      val estLocalDateTime = dateTime.toLocalDateTime
      val utcLocalDateTime = dateTime.utcLocalDateTime

      estLocalDateTime === new LocalDateTime(2014, 1, 1, 1, 10, 58)
      utcLocalDateTime === new LocalDateTime(2013, 12, 31, 12, 10, 58)
    }

    "support conversion to UTC LocalDate" in {
      val localDate = new LocalDate("2014-01-01")
      val dateTime = localDate.toDateTimeAtStartOfDay.toDateTime(DateTimeZone.forID("EST"))
      val estLocalDate = dateTime.toLocalDate
      val utcLocalDate = dateTime.utcLocalDate

      estLocalDate === new LocalDate(2013, 12, 31)
      utcLocalDate === new LocalDate(2014, 1, 1)
    }

    "support same day comparison" in {
      val dateTimeStart = new LocalDate("2014-01-01").toDateTimeAtStartOfDay
      val dateTimeEnd = new LocalDate("2014-01-01").toDateTimeAtEndOfDay
      val dateTimeOther = new LocalDate("2014-01-02").toDateTimeAtStartOfDay

      dateTimeStart.isSameDay(dateTimeEnd) must beTrue
      dateTimeOther.isSameDay(dateTimeEnd) must beFalse
      dateTimeOther.isSameDay(dateTimeStart) must beFalse
    }

    "be able to check if a date time is between two date times" in {
      val dateTimeStart = new LocalDate("2014-01-01").toDateTimeAtStartOfDay
      val dateTimeEnd = new LocalDate("2014-01-01").toDateTimeAtEndOfDay
      val dateTimeOther = new LocalDate("2014-01-02").toDateTimeAtStartOfDay

      dateTimeStart.between(dateTimeEnd, dateTimeOther) must beFalse
      dateTimeEnd.between(dateTimeStart, dateTimeOther) must beTrue
      dateTimeStart.between(dateTimeOther, dateTimeEnd) must beFalse
    }

    "support iterable interval creation" in {
      val dateTime1Start = new LocalDate("2014-01-01").toDateTimeAtStartOfDay
      val dateTime2Start = new LocalDate("2014-01-02").toDateTimeAtStartOfDay
      val dateTime3Start = new LocalDate("2014-01-03").toDateTimeAtStartOfDay
      val dateTime3End = new LocalDate("2014-01-03").toDateTimeAtEndOfDay

      dateTime1Start.to(dateTime1Start).toList === List(dateTime1Start)
      dateTime1Start.to(dateTime3Start).toList === List(dateTime1Start, dateTime2Start, dateTime3Start)
      dateTime1Start.to(dateTime3End).toList === List(dateTime1Start, dateTime2Start, dateTime3Start)
      dateTime3End.to(dateTime1Start) must throwAn[IllegalArgumentException]

      dateTime1Start.until(dateTime1Start).toList === List.empty
      dateTime1Start.until(dateTime3Start).toList === List(dateTime1Start, dateTime2Start)
      dateTime1Start.until(dateTime3End).toList === List(dateTime1Start, dateTime2Start, dateTime3Start)
      dateTime3End.until(dateTime1Start) must throwAn[IllegalArgumentException]
    }
  }

  "An ApsoTimeInterval" should {
    "split itself into subsequences" in {
      val interval = new Interval(0, 100)
      interval.split(0) === Seq.empty
      interval.split(-1) must throwAn[IllegalArgumentException]
      interval.split(5).size === 5
      interval.split(4) === Seq(new Interval(0, 25), new Interval(25, 50), new Interval(50, 75), new Interval(75, 100))
    }
  }
}
