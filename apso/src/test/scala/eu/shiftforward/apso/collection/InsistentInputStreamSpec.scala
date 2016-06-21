package eu.shiftforward.apso.collection

import java.io.InputStream

import org.specs2.mutable._

class InsistentInputStreamSpec extends Specification {

  "An InsistentInputStream" should {

    val testList = List[Byte](1, 2, 3, 4, 5)

    def testGoodInputStream() = new InputStream {
      val iter = testList.iterator
      def read(): Int = iter.next()
    }

    def testBuggyInputStream() = new InputStream {
      var fail = false
      val iter = testList.iterator
      def read(): Int = {
        if (fail) throw new Exception("bad read")
        else {
          fail = true
          iter.next()
        }
      }
      override def skip(l: Long) = {
        iter.drop(l.toInt)
        l
      }
    }

    def testBadInputStream() = new InputStream {
      val iter = testList.iterator
      def read(): Int = throw new Exception("bad read")
    }

    "have a working input stream interface" in {

      val stream = new InsistentInputStream(testGoodInputStream)

      stream.read() === 1
      stream.read() === 2
      stream.skip(2) === 2
      stream.read() === 5
    }

    "retry reads when a inner stream is buggy" in {
      val buggyStream = testBuggyInputStream()

      buggyStream.read() === 1
      buggyStream.read() must throwAn[Exception]

      val stream = new InsistentInputStream(testBuggyInputStream)

      stream.read() === 1
      stream.read() === 2
      stream.skip(2) === 2
      stream.read() === 5
    }

    "fail after the retry limit is exceeded" in {
      val stream = new InsistentInputStream(testBadInputStream)
      stream.read() must throwAn[Exception]
    }
  }

}