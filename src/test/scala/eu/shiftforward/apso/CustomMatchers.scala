package eu.shiftforward.apso

import java.io._
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationLike
import scala.reflect.ClassTag

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
}

