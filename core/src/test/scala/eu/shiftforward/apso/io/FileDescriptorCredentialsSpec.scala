package eu.shiftforward.apso.io

import com.typesafe.config.{ ConfigFactory, Config }
import eu.shiftforward.apso.CustomMatchers
import eu.shiftforward.apso.config.FileDescriptorCredentials
import org.specs2.mutable.Specification

class FileDescriptorCredentialsSpec extends Specification with CustomMatchers {

  val fdCredentials = new FileDescriptorCredentials[(String, String)] {
    def protocol: String = "test-protocol"
    def id(path: String): String = path
    def createCredentials(id: String, fdConfig: Config): (String, String) =
      (fdConfig.getString("username"), fdConfig.getString("password"))
  }

  val baseConfig = ConfigFactory.parseString(
    """
      |test-protocol {
      |  credentials = [{
      |    id = "foo"
      |    creds {
      |      username = "foo-username"
      |      password = "foo-password"
      |    }
      |  },{
      |    ids = ["bar", "zed"]
      |    creds {
      |      username = "barzed-username"
      |      password = "barzed-password"
      |    }
      |  },{
      |    id = "a"
      |    ids = ["b", "c"]
      |    creds {
      |      username = "ids-username"
      |      password = "ids-password"
      |    }
      |  },{
      |    id = "repeated"
      |    creds {
      |      username = "repeated-username"
      |      password = "repeated-password"
      |    }
      |  },{
      |    id = "repeated"
      |    creds {
      |      username = "repeated-2-username"
      |      password = "repeated-2-password"
      |    }
      |  }]
      |}
    """.stripMargin)

  val withDefaultConfig = ConfigFactory.parseString(
    """
      |test-protocol.default {
      |  username = "default-username"
      |  password = "default-password"
      |}
    """.stripMargin)

  val testConfig = baseConfig.withFallback(withDefaultConfig)

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

    "read the array notation over the single notation in the config when both are defined" in {
      val path = "b"
      val creds = fdCredentials.read(baseConfig, path)
      creds must beSome(("ids-username", "ids-password"))

      val path2 = "a"
      val creds2 = fdCredentials.read(baseConfig, path2)
      creds2 must beNone
    }

    "if the fd's is matched multiple times in the config, read the first appearance" in {
      val path = "repeated"
      val creds = fdCredentials.read(testConfig, path)
      creds must beSome(("repeated-username", "repeated-password"))
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
