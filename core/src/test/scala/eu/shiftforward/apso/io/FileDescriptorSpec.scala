package eu.shiftforward.apso.io

import com.amazonaws.auth.BasicAWSCredentials
import eu.shiftforward.apso.CustomMatchers
import eu.shiftforward.apso.aws.S3Bucket
import org.specs2.mutable.Specification

import scala.util.Try

class FileDescriptorSpec extends Specification with CustomMatchers {

  "A FileDescriptor" should {
    val fdConfig = config.Credentials(
      s3 = config.Credentials.S3(
        ids = Map("test" -> config.Credentials.S3.Entry("a", "b"))),
      sftp = config.Credentials.Sftp(
        default = Some(config.Credentials.Sftp.Entry.Basic("foo", "bar"))))

    "correctly be initialized given a URI with protocol" in {
      FileDescriptor("file:///tmp/folder") mustEqual LocalFileDescriptor("/tmp/folder")
      FileDescriptor("s3://tmp/path") mustEqual S3FileDescriptor("tmp/path")
      FileDescriptor("sftp://localhost/tmp/path", fdConfig) mustEqual SftpFileDescriptor("localhost/tmp/path", fdConfig.sftp)
      FileDescriptor("sftp://valid-host.com/tmp/path", fdConfig) mustEqual SftpFileDescriptor("valid-host.com/tmp/path", fdConfig.sftp)
    }

    "be serializable" in {
      FileDescriptor("file:///tmp/folder") must beSerializable
      FileDescriptor("s3://tmp/path") must beSerializable
      FileDescriptor("sftp://localhost/tmp/path", fdConfig) must beSerializable
    }

    "fail when initializing with an unsupported protocol" in {
      Try(FileDescriptor("wrongprotocol:///tmp")) must beAFailedTry
    }

    "fail when initializing without a protocol" in {
      Try(FileDescriptor("tmp")) must beAFailedTry
    }

    "be initialized with credentials when given a config" in {
      FileDescriptor("s3://test/path/path", fdConfig) match {
        case s3: S3FileDescriptor =>
          s3 must beSerializable
          s3.bucket must beEqualTo(new S3Bucket("test", () => new BasicAWSCredentials("a", "b")))
      }
    }
  }
}
