package com.kevel.apso

import scala.concurrent._
import scala.concurrent.duration._

import org.specs2.execute.AsResult
import org.specs2.matcher._
import org.specs2.mutable.SpecificationLike

trait FutureExtraMatchers { this: SpecificationLike =>

  implicit class RichAwaitable[T](val awaitable: Awaitable[T]) {
    def get = Await.result(awaitable, 1.second)
    def await(timeout: Duration) = Await.result(awaitable, timeout)
  }
}
