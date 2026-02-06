package com.kevel.apso.io

import java.io.{FileDescriptor as _, *}

import scala.reflect.ClassTag
import scala.util.Try

import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}

import com.kevel.apso.aws.S3Bucket

class FileDescriptorSpec extends Specification {

  def beSerializable[T <: AnyRef: ClassTag]: Matcher[T] = (obj: T) => {
    val buffer = new ByteArrayOutputStream(10000)
    val out = new ObjectOutputStream(buffer)
    out.writeObject(obj) must
      (not(throwA[NotSerializableException]) and not(throwAn[InvalidClassException]))
  }

  "A FileDescriptor" should {
    val fdConfig = config.Credentials(
      s3 = config.Credentials.S3(ids = Map("test" -> config.Credentials.S3.Entry("a", "b"))),
      sftp = config.Credentials.Sftp(default = Some(config.Credentials.Sftp.Entry.Basic("foo", "bar")))
    )

    "correctly be initialized given a URI with protocol" in {
      FileDescriptor("file:///tmp/folder") mustEqual LocalFileDescriptor("/tmp/folder")
      FileDescriptor("s3://tmp/path") mustEqual S3FileDescriptor("tmp/path")
      FileDescriptor("sftp://localhost/tmp/path", fdConfig) mustEqual SftpFileDescriptor(
        "localhost/tmp/path",
        fdConfig.sftp
      )
      FileDescriptor("sftp://valid-host.com/tmp/path", fdConfig) mustEqual SftpFileDescriptor(
        "valid-host.com/tmp/path",
        fdConfig.sftp
      )
    }

    "correctly identify itself as local or non-local" in {
      FileDescriptor("file:///tmp/folder").isLocal must beTrue
      FileDescriptor("s3://tmp/path").isLocal must beFalse
      FileDescriptor("sftp://localhost/tmp/path", fdConfig).isLocal must beFalse
      FileDescriptor("sftp://valid-host.com/tmp/path", fdConfig).isLocal must beFalse
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
          s3.bucket must beEqualTo(
            new S3Bucket("test", () => StaticCredentialsProvider.create(AwsBasicCredentials.create("a", "b")))
          )
      }
    }
  }
}
