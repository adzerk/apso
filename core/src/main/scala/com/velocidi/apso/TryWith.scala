package com.velocidi.apso

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

// This is a transcript from https://codereview.stackexchange.com/a/115767/67025
/** Mimics the try-with-resource construct from Java world, or a loan pattern, where a given function can try to use a
  * Closeable resource which shall be disposed off and closed properly afterwards.
  */
object TryWith {
  def apply[C <: AutoCloseable, R](resource: => C)(f: C => R): Try[R] = {
    Try(resource).flatMap(resourceInstance => {
      try {
        val returnValue = f(resourceInstance)
        Try(resourceInstance.close()).map(_ => returnValue)
      } catch {
        case NonFatal(exceptionInFunction) =>
          try {
            resourceInstance.close()
            Failure(exceptionInFunction)
          } catch {
            case NonFatal(exceptionInClose) =>
              exceptionInFunction.addSuppressed(exceptionInClose)
              Failure(exceptionInFunction)
          }
      }
    })
  }
}
