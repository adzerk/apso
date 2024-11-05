package com.kevel.apso.io

import java.net.URI

import org.specs2.mutable.Specification

class S3FileDescriptorSpec extends Specification {
  "A S3FileDescriptor" should {
    "have a correct URI that exposes the full path" in {
      val file = S3FileDescriptor("bucket/key")
      file.uri ==== new URI("s3://bucket/key")
    }
  }
}
