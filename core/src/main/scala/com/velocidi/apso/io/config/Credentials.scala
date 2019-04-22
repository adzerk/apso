package com.velocidi.apso.io.config

case class Credentials(
    sftp: Credentials.Sftp = Credentials.Sftp(),
    s3: Credentials.S3 = Credentials.S3())

object Credentials {

  trait Protocol[T] {
    def default: Option[T]
    def ids: Map[String, T]
  }

  case class S3(default: Option[S3.Entry] = None, ids: Map[String, S3.Entry] = Map()) extends Protocol[S3.Entry]

  object S3 {
    case class Entry(accessKey: String, secretKey: String, roleArn: Option[String] = None)
  }

  case class Sftp(default: Option[Sftp.Entry] = None, ids: Map[String, Sftp.Entry] = Map()) extends Protocol[Sftp.Entry]

  object Sftp {
    sealed trait Entry

    object Entry {
      case class Basic(username: String, password: String) extends Entry
      case class PublicKey(username: String, keypairFile: String, passphrase: Option[String]) extends Entry
    }
  }
}
