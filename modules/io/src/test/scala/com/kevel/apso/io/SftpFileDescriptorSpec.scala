package com.kevel.apso.io

import java.io.File
import java.net.URI

import scala.util.Properties

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

    "resolve a PublicKey config to an identity rooted at ~/.ssh/" in {
      val file = SftpFileDescriptor(
        "localhost/tmp/file",
        Credentials.Sftp(default =
          Some(Credentials.Sftp.Entry.PublicKey(username = "u", keypairFile = "id_rsa", passphrase = Some("phrase")))
        )
      )
      file.identity ==== Some(
        SftpFileDescriptor.Identity(Right(new File(Properties.userHome + "/.ssh/id_rsa")), Some("phrase"))
      )
    }

    "resolve a PublicKeyContent config to an in-memory key identity without filesystem lookup" in {
      val pem =
        """-----BEGIN OPENSSH PRIVATE KEY-----
          |b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAAB
          |-----END OPENSSH PRIVATE KEY-----""".stripMargin
      val file = SftpFileDescriptor(
        "localhost/tmp/file",
        Credentials.Sftp(default =
          Some(Credentials.Sftp.Entry.PublicKeyContent(username = "u", privateKey = pem, passphrase = None))
        )
      )
      file.identity ==== Some(SftpFileDescriptor.Identity(Left(pem), None))
    }
  }
}
