package com.kevel.apso.time

import org.joda.time.{DateTime, Period}
import org.specs2.mutable._

import Implicits._

class IterableIntervalSpec extends Specification {

  "An iterable interval" should {

    "return the appropriate length with the default step" in {
      (new DateTime("2012-01-01") to new DateTime("2012-01-01")).length mustEqual 1
      (new DateTime("2012-01-01") until new DateTime("2012-01-01")).length mustEqual 0

      (new DateTime("2012-01-01") to new DateTime("2012-01-02")).length mustEqual 2
      (new DateTime("2012-01-01") until new DateTime("2012-01-02")).length mustEqual 1

      (new DateTime("2012-01-01") until new DateTime("2012-02-01")).length mustEqual 31
      (new DateTime("2012-02-01") until new DateTime("2012-03-01")).length mustEqual 29
    }

    "return the appropriate length with other steps" in {
      (new DateTime("2012-02-01") until new DateTime("2012-03-01") by Period.days(1)).length mustEqual 29

      (new DateTime("2012-01-01") until new DateTime("2012-02-01") by Period.months(1)).length mustEqual 1
      (new DateTime("2012-02-01") until new DateTime("2012-03-01") by Period.months(1)).length mustEqual 1

      (new DateTime("2012-01-01") until new DateTime("2012-02-01") by Period.minutes(
        2
      )).length mustEqual 31 * 24 * 60 / 2
      (new DateTime("2012-02-01") until new DateTime("2012-03-01") by Period.minutes(
        2
      )).length mustEqual 29 * 24 * 60 / 2

      (new DateTime("2012-02-01") until new DateTime("2012-03-01") by Period.years(1)).length mustEqual 1
      (new DateTime("2012-02-01") to new DateTime("2012-03-01") by Period.years(1)).length mustEqual 1
    }

    "return the correct values while iterating with the default step" in {
      (new DateTime("2012-01-01") to new DateTime("2012-01-01")).toSeq mustEqual Seq(new DateTime("2012-01-01"))
      (new DateTime("2012-01-01") until new DateTime("2012-01-01")).toSeq mustEqual Seq()

      (new DateTime("2012-01-01") until new DateTime("2012-01-03")).toSeq mustEqual
        Seq(new DateTime("2012-01-01"), new DateTime("2012-01-02"))

      (new DateTime("2012-01-29") until new DateTime("2012-02-02")).toSeq mustEqual
        Seq(
          new DateTime("2012-01-29"),
          new DateTime("2012-01-30"),
          new DateTime("2012-01-31"),
          new DateTime("2012-02-01")
        )

      (new DateTime("2012-02-29") until new DateTime("2012-03-02")).toSeq mustEqual
        Seq(new DateTime("2012-02-29"), new DateTime("2012-03-01"))
    }

    "return the correct values while iterating with other steps" in {
      (new DateTime("2012-01-01") until new DateTime("2012-01-01") by Period.days(1)).toSeq mustEqual Seq()

      (new DateTime("2012-01-01") until new DateTime("2012-01-03") by Period.days(2)).toSeq mustEqual
        Seq(new DateTime("2012-01-01"))

      (new DateTime(2012, 1, 29, 1, 0) until new DateTime(2012, 1, 29, 1, 10) by Period.minutes(2)).toSeq mustEqual
        Seq(
          new DateTime(2012, 1, 29, 1, 0),
          new DateTime(2012, 1, 29, 1, 2),
          new DateTime(2012, 1, 29, 1, 4),
          new DateTime(2012, 1, 29, 1, 6),
          new DateTime(2012, 1, 29, 1, 8)
        )
    }
  }
}
