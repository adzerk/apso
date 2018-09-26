package com.velocidi.apso.io

import java.io.ByteArrayInputStream
import scala.io.Source
import org.specs2.mutable._

class LazySequenceInputStreamSpec extends Specification {

  "A lazy sequence input stream" should {

    "combine input streams correctly (read byte-by-byte)" in {
      var open = 0

      val streams = List[Byte](0, 1, 2, 3).map { byte => () =>
        open += 1
        new ByteArrayInputStream(Array[Byte](byte))
      }

      val is = new LazySequenceInputStream(streams)
      is.read === 0
      open === 1
      is.read === 1
      open === 2
      is.read === 2
      open === 3
      is.read === 3
      open === 4
      is.read === -1
    }

    "combine input streams correctly (read byte array)" in {
      val streams = List[Byte](5, 1, 2, 3).map { byte => () =>
        new ByteArrayInputStream(Array[Byte](byte))
      }

      val is = new LazySequenceInputStream(streams)
      val buf = new Array[Byte](4)

      is.read(buf, 0, 4)

      buf === Array[Byte](5, 0, 0, 0)

      is.read(buf, 1, 3)

      buf === Array[Byte](5, 1, 0, 0)
    }

    "combine input streams correctly (read incomplete byte array)" in {
      val streams = (1 to 12).map(_.toByte).grouped(4).toList.map { bs => () =>
        new ByteArrayInputStream(bs.toArray)
      }
      val is = new LazySequenceInputStream(streams)
      val buf = new Array[Byte](9)

      val next = is.read(buf, 0, 7)

      buf === Array[Byte](1, 2, 3, 4, 0, 0, 0, 0, 0)

      is.read(buf, next, 7 - next)

      buf === Array[Byte](1, 2, 3, 4, 5, 6, 7, 0, 0)
    }

    "combine input streams correctly (read interface)" in {
      val streams = "testqwer".toSeq.map(_.toByte).grouped(4).toList.map { bs => () =>
        new ByteArrayInputStream(bs.toArray)
      }
      val is = new LazySequenceInputStream(streams)

      Source.fromInputStream(is).getLines().toList.head === "testqwer"
    }

  }
}
