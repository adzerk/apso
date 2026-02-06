package com.kevel.apso.io

import java.io.InputStream

import org.specs2.mutable._

class InsistentInputStreamSpec extends Specification {

  abstract class FailNextInputStream extends InputStream {
    var fail = false
    def failNext() = fail = true
  }

  "An InsistentInputStream" should {

    val testList = (1 to 100).map(_.toByte)

    val testGoodInputStream = () =>
      new InputStream {
        val iter = testList.iterator
        def read(): Int = iter.next()
      }

    val testBuggyInputStream = () =>
      new FailNextInputStream {
        val iter = testList.iterator
        def read(): Int = {
          if (fail) {
            fail = false
            throw new Exception("bad read")
          } else {
            iter.next()
          }
        }
        override def skip(l: Long) = {
          iter.drop(l.toInt)
          l
        }
      }

    val testOptimizedBuggyInputStream = (offset: Long) =>
      new FailNextInputStream {
        val iter = testList.drop(offset.toInt).iterator
        def read(): Int = {
          if (fail) {
            fail = false
            throw new Exception("bad read")
          } else {
            iter.next()
          }
        }
        override def skip(l: Long): Long = throw new Exception("unoptimized skip")
      }

    val testBadInputStream = () =>
      new InputStream {
        val iter = testList.iterator
        def read(): Int = throw new Exception("bad read")
      }

    "have a working input stream interface" in {

      val stream = new InsistentInputStream(testGoodInputStream)
      val testBuffer = Array[Byte](0, 0, 0)

      stream.read() === 1
      stream.read() === 2
      stream.skip(2) === 2
      stream.read() === 5
      stream.read(testBuffer) === 3
      testBuffer === Array[Byte](6, 7, 8)
      stream.skip(1) === 1
      stream.read() === 10
      stream.read(testBuffer, 1, 2) === 2
      testBuffer === Array[Byte](6, 11, 12)
    }

    "retry reads when a inner stream is buggy" in {
      val buggyStream = testBuggyInputStream()

      buggyStream.read() === 1
      buggyStream.failNext()
      buggyStream.read() must throwAn[Exception]

      val stream = new InsistentInputStream(testBuggyInputStream)
      val testBuffer = Array[Byte](0, 0, 0)

      stream.read() === 1
      buggyStream.failNext()
      stream.read() === 2
      stream.skip(2) === 2
      buggyStream.failNext()
      stream.read() === 5
      buggyStream.failNext()
      stream.read(testBuffer) === 3
      testBuffer === Array[Byte](6, 7, 8)
      stream.skip(1) === 1
      buggyStream.failNext()
      stream.read() === 10
      buggyStream.failNext()
      stream.read(testBuffer, 1, 2) === 2
      testBuffer === Array[Byte](6, 11, 12)
    }

    "retry reads when a inner stream is buggy using an optimized skip" in {
      val buggyStream = testOptimizedBuggyInputStream(3)

      buggyStream.read() === 4
      buggyStream.failNext()
      buggyStream.read() must throwAn[Exception]

      val stream = new InsistentInputStream(testBuggyInputStream)
      val testBuffer = Array[Byte](0, 0, 0)

      stream.read() === 1
      buggyStream.failNext()
      stream.read() === 2
      buggyStream.failNext()
      stream.read() === 3
      stream.read() === 4
      buggyStream.failNext()
      stream.read(testBuffer) === 3
      testBuffer === Array[Byte](5, 6, 7)
      buggyStream.failNext()
      stream.read() === 8
      stream.read() === 9
      buggyStream.failNext()
      stream.read(testBuffer, 1, 2) === 2
      testBuffer === Array[Byte](5, 10, 11)
    }

    "fail after the retry limit is exceeded" in {
      val stream = new InsistentInputStream(testBadInputStream)
      stream.read() must throwAn[Exception]
    }
  }

}
