package com.kevel.apso.io

import org.specs2.mutable.Specification

class FileDescriptorCredentialsSpec extends Specification {

  val fdCredentials = new FileDescriptorCredentials[config.Credentials.Sftp.Entry, (String, String)] {
    def id(path: String): String = path

    def createCredentials(id: String, fdConfig: config.Credentials.Sftp.Entry): (String, String) = fdConfig match {
      case config.Credentials.Sftp.Entry.Basic(username, password) => (username, password)
      case _                                                       => throw new Exception
    }
  }

  val baseConfig = config.Credentials.Sftp(
    ids = Map(
      "foo" -> config.Credentials.Sftp.Entry.Basic("foo-username", "foo-password"),
      "bar" -> config.Credentials.Sftp.Entry.Basic("barzed-username", "barzed-password"),
      "zed" -> config.Credentials.Sftp.Entry.Basic("barzed-username", "barzed-password")
    )
  )

  val testConfig =
    baseConfig.copy(default = Some(config.Credentials.Sftp.Entry.Basic("default-username", "default-password")))

  "A FileDescriptorCredential" should {

    "read the credentials for an existing fd's id" in {
      val path = "foo"
      val creds = fdCredentials.read(testConfig, path)
      creds must beSome(("foo-username", "foo-password"))
    }

    "read the credentials when it is defined in the array notation in the config" in {
      val path = "bar"
      val creds = fdCredentials.read(testConfig, path)
      creds must beSome(("barzed-username", "barzed-password"))

      val path2 = "zed"
      val creds2 = fdCredentials.read(testConfig, path2)
      creds2 must beSome(("barzed-username", "barzed-password"))
    }

    "read the default config when the fd's id isn't matched" in {
      val path = "missing"
      val creds = fdCredentials.read(testConfig, path)
      creds must beSome(("default-username", "default-password"))
    }

    "do not read the credentials when fd's id isn't matched and a default wasn't defined" in {
      val path = "missing"
      val creds = fdCredentials.read(baseConfig, path)
      creds must beNone
    }

    "do not read the credentials if the given id is empty" in {
      val path = ""
      val creds = fdCredentials.read(baseConfig, path)
      creds must beNone
    }

    "read the credentials if the given id is empty but a default for the protocol is defined" in {
      val path = ""
      val creds = fdCredentials.read(testConfig, path)
      creds must beSome(("default-username", "default-password"))
    }
  }
}
