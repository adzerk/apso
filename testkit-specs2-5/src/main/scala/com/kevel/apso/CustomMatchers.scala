package com.kevel.apso

import java.io._

import scala.reflect.ClassTag

import org.specs2.execute.{NoDetails, Result}
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable.SpecificationLike

trait CustomMatchers extends SpecificationLike {
  def serializationBufSize = 10000

  def beSerializable[T <: AnyRef: ClassTag]: Matcher[T] = (obj: T) => {
    val buffer = new ByteArrayOutputStream(serializationBufSize)

    val out = new ObjectOutputStream(buffer)
    out.writeObject(obj) must
      (not(throwA[NotSerializableException]) and not(throwAn[InvalidClassException]))
  }

  def exist: Matcher[File] = new Matcher[File] {
    def apply[S <: File](v: Expectable[S]) = {
      Result.result(v.value.exists(), v.value.getName + " exists", v.value.getName + " does not exist", v.toString)
    }
  }

  /** Return a successful MatchResult[T]. This is useful to explicitly expose a value outside a Matcher which can later
    * be accessed with `_.expectable.value`.
    */
  def offer[T](result: T): Result = Result.result(test = true, "ok", result.toString, NoDetails)
}
