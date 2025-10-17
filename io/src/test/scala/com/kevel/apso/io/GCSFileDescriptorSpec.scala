package com.kevel.apso.io

import java.net.URI

import org.specs2.mutable.Specification

class GCSFileDescriptorSpec extends Specification {
  "A GCSFileDescriptor" should {
    "have a correct URI that exposes the full path" in {
      val file = GCSFileDescriptor("bucket/key")
      file.uri ==== new URI("gs://bucket/key")
    }
  }
}
