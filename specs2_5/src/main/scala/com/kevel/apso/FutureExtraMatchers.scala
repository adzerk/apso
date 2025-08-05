package com.kevel.apso

import scala.concurrent._
import scala.concurrent.duration._

import org.specs2.mutable.SpecificationLike

trait FutureExtraMatchers { this: SpecificationLike =>

  extension [T](awaitable: Awaitable[T]) {
    def get = Await.result(awaitable, 1.second)
    def await(timeout: Duration) = Await.result(awaitable, timeout)
  }
}
