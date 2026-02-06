package com.kevel.apso.io

import java.net.URI

import org.specs2.mutable.Specification

import com.kevel.apso.io.config.Credentials

class SftpFileDescriptorSpec extends Specification {
  "A SftpFileDescriptor" should {
    "have a correct URI that exposes the full path" in {
      val file = SftpFileDescriptor(
        "localhost/tmp/file",
        Credentials.Sftp(default = Some(Credentials.Sftp.Entry.Basic(username = "user123", password = "pass456")))
      )
      file.uri ==== new URI("sftp://user123@localhost/tmp/file")
    }
  }
}
