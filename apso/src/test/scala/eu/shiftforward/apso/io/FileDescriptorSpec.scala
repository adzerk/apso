package eu.shiftforward.apso.io

import eu.shiftforward.apso.CustomMatchers
import org.specs2.mutable.Specification

import scala.util.Try

class FileDescriptorSpec extends Specification with CustomMatchers {

  "A FileDescriptor" should {

    "Correctly be initialized given a URI with protocol" in {
      FileDescriptor("file:///tmp/folder") mustEqual LocalFileDescriptor("/tmp/folder")
      FileDescriptor("s3://tmp/path") mustEqual S3FileDescriptor("tmp/path")
    }

    "Be seriablizable" in {
      FileDescriptor("file:///tmp/folder") must beSerializable
      FileDescriptor("s3://tmp/path") must beSerializable
    }

    "Fail when initializing with an unsupported protocol" in {
      Try(FileDescriptor("wrongprotocol:///tmp")) must beAFailedTry
    }

    "Fail when initializing without a prococol" in {
      Try(FileDescriptor("tmp")) must beAFailedTry
    }

  }
}
