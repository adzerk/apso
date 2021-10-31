package com.velocidi.apso

import java.io._

import scala.reflect.ClassTag

import org.specs2.matcher.{Expectable, MatchResult, Matcher}
import org.specs2.mutable.SpecificationLike

trait CustomMatchers extends SpecificationLike {
  def serializationBufSize = 10000

  def beSerializable[T <: AnyRef: ClassTag]: Matcher[T] = { obj: T =>
    val buffer = new ByteArrayOutputStream(serializationBufSize)

    val out = new ObjectOutputStream(buffer)
    out.writeObject(obj) must
      not(throwA[NotSerializableException]) and not(throwAn[InvalidClassException])
  // val in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray))
  // in.readObject() must beAnInstanceOf[T] and
  //   not(throwA[InvalidClassException]) and not(throwA[StreamCorruptedException])
  }

  def exist: Matcher[File] = new Matcher[File] {
    def apply[S <: File](v: Expectable[S]) = {
      result(v.value.exists(), v.value.getName + " exists", v.value.getName + " does not exist", v)
    }
  }

  /** Return a successful MatchResult[T]. This is useful to explicitly expose a value outside a Matcher which can later
    * be accessed with `_.expectable.value`.
    */
  def offer[T](result: T): MatchResult[T] = Matcher.result(test = true, "ok", createExpectable(result))
}
