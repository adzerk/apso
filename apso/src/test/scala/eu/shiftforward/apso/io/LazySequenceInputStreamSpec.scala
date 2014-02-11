package eu.shiftforward.apso.io

import java.io.ByteArrayInputStream
import org.specs2.mutable._

class LazySequenceInputStreamSpec extends Specification {
  "A lazy sequence input stream" should {
    val streams = List[Byte](0, 1, 2, 3).map { byte =>
      () => new ByteArrayInputStream(Array[Byte](byte))
    }

    def isFactory = new LazySequenceInputStream(streams)

    "combine input streams correctly (read byte-by-byte)" in {
      val is = isFactory
      is.read === 0
      is.read === 1
      is.read === 2
      is.read === 3
      is.read === -1
    }

    "combine input streams correctly (read byte array)" in {
      val is = isFactory
      val buf = new Array[Byte](4)

      is.read(buf, 0, 4)

      buf === Array[Byte](0, 1, 2, 3)
    }

    "combine input streams correctly (read incomplete byte array)" in {
      val streams = (1 to 12).map(_.toByte).grouped(4).toList.map {
        bs => () => new ByteArrayInputStream(bs.toArray)
      }
      val is = new LazySequenceInputStream(streams)
      val buf = new Array[Byte](9)

      is.read(buf, 0, 7)

      buf === Array[Byte](1, 2, 3, 4, 5, 6, 7, 0, 0)
    }

  }
}
