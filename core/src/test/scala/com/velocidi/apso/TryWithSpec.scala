package com.velocidi.apso

import java.io.Closeable

import scala.util.{Failure, Success}

import org.specs2.mutable.Specification

// Based on: https://codereview.stackexchange.com/a/115767/67025
class TryWithSpec extends Specification {

  // Exceptions and errors here so we don't pay the stack trace creation cost multiple times
  val getResourceException = new RuntimeException
  val inFunctionException = new RuntimeException
  val inCloseException = new RuntimeException
  val getResourceError = new OutOfMemoryError
  val inFunctionError = new OutOfMemoryError
  val inCloseError = new OutOfMemoryError

  val goodResource = new Closeable {
    override def toString: String = "good resource"
    def close(): Unit = {}
  }

  "TryWith" should {
    "catch exceptions getting the resource" in {
      TryWith(throw getResourceException)(println) must beEqualTo(Failure(getResourceException))
    }

    "catch exceptions in the function" in {
      TryWith(goodResource)(_ => throw inFunctionException) must beEqualTo(Failure(inFunctionException))
    }

    "catch exceptions while closing" in {
      TryWith(new Closeable {
        def close(): Unit = throw inCloseException
      })(_.toString) must beEqualTo(Failure(inCloseException))
    }

    "note suppressed exceptions" in {
      val ex = new RuntimeException
      val result = TryWith(new Closeable {
        def close(): Unit = throw inCloseException
      })(_ => throw ex)

      result must beAFailedTry.which { returnedException =>
        returnedException mustEqual ex
        returnedException.getSuppressed must beEqualTo(Array(inCloseException))
      }
    }

    "propagate errors getting the resource" in {
      TryWith(throw getResourceError)(println) must throwAn(getResourceError)
    }

    "propagate errors in the function" in {
      TryWith(goodResource)(_ => throw inFunctionError) must throwAn(inFunctionError)
    }

    "propagate errors while closing" in {
      TryWith(new Closeable {
        def close(): Unit = throw inCloseError
      })(_.toString) must throwAn(inCloseError)
    }

    "return the value from a successful run" in {
      TryWith(goodResource)(_.toString) must beEqualTo(Success("good resource"))
    }
  }
}
