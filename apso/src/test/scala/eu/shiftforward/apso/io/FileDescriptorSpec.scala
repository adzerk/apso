package eu.shiftforward.apso.io

import org.specs2.mutable.Specification

import scala.util.Try

class FileDescriptorSpec extends Specification {

  "A FileDescriptor" should {

    "Correctly be initialized given a URI with protocol" in {
      FileDescriptor("file:///tmp/folder") mustEqual LocalFileDescriptor("/tmp/folder")
      FileDescriptor("s3://tmp/path") mustEqual S3FileDescriptor("tmp/path")
    }

    "Fail when initializing with an unsupported protocol" in {
      Try(FileDescriptor("wrongprotocol:///tmp")) must beAFailedTry
    }

    "Fail when initializing without a prococol" in {
      Try(FileDescriptor("tmp")) must beAFailedTry
    }

  }
}
