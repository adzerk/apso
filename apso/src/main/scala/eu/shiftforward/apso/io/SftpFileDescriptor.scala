package eu.shiftforward.apso.io

import com.jcraft.jsch.JSchException
import com.typesafe.config.Config
import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.config.FileDescriptorCredentials
import eu.shiftforward.apso.config.Implicits._
import fr.janalyse.ssh._
import java.io.File

case class SftpFileDescriptor(
    private val sshOptions: SSHOptions,
    protected val elements: List[String]) extends FileDescriptor with RemoteFileDescriptor with Logging {

  protected def root = "/"

  private def username = sshOptions.username
  private def host = sshOptions.host

  private def ssh[A](block: SSH => A): A = {
    def doConnect(retries: Int): A = {
      try {
        SSH.once(sshOptions)(block)
      } catch {
        case e: JSchException if retries > 0 =>
          log.warn("[{}] {}. Retrying in 5 seconds...", host, e.getMessage, null)
          Thread.sleep(5000)
          doConnect(retries - 1)
      }
    }

    doConnect(3)
  }

  protected def copy(elements: List[String]) =
    this.copy(elements = elements)

  def exists: Boolean = ssh(_.exists(path))
  def isDirectory: Boolean = ssh(_.isDirectory(path))

  def list: Iterator[FileDescriptor] =
    if (isDirectory) {
      ssh(_.ls(path)).toIterator.map(this.child)
    } else {
      Iterator()
    }

  def listAllFilesWithPrefix(prefix: String): Iterator[FileDescriptor] = {
    def aux(f: FileDescriptor): Iterator[FileDescriptor] =
      if (f.isDirectory) f.list.flatMap(aux)
      else Iterator(f)

    this.list.filter(_.name.startsWith(prefix)).flatMap(aux)
  }

  def delete(): Boolean =
    if (exists) {
      if (isDirectory) ssh(_.rmdir(path))
      else { ssh(_.rm(path)); true }

    } else false

  def mkdirs(): Boolean = exists || ssh(_.mkdir(path))

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    if (isDirectory || localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {
      val downloadFile = if (safeDownloading) localTarget.sibling(_ + ".tmp") else localTarget
      localTarget.parent().mkdirs()
      ssh(_.receive(this.path, new File(downloadFile.path)))

      // FIXME: currently the JASSH API provides no way to know wheter a file download was successful
      // there's an open issue to improve this (https://github.com/dacr/jassh/issues/16)
      val success = downloadFile.exists

      if (success && safeDownloading) downloadFile.rename(localTarget)

      success
    }
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    if (isDirectory || localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {
      try {
        ssh(_.send(localTarget.file, path))
        true

      } catch {
        case e: Throwable => false
      }
    }
  }

  override def toString: String = s"sftp://$username@$host$path"
}

object SftpFileDescriptor {
  def apply(path: String, sshOptions: SSHOptions): SftpFileDescriptor =
    path.split("/").toList match {
      case Nil => SftpFileDescriptor(sshOptions, Nil)
      case "" :: hd :: tail => SftpFileDescriptor(sshOptions, hd :: tail)
      case _ =>
        throw new IllegalArgumentException("Error parsing SFTP URI. Only absolute paths are supported.")
    }

  val credentials = new FileDescriptorCredentials[(String, String, Int, Either[String, String])] {
    val idRegex = """(.*@)?(\d*|\w*|\.*)\/(.*)""".r

    def id(path: String) = path match {
      case idRegex(id, _) => id
      case idRegex(_, id, _) => id
      case _ => throw new IllegalArgumentException("Error parsing SFTP URI.")
    }

    val protocol = "sftp"

    def createCredentials(hostname: String, sftpConfig: Config) = {
      val username = sftpConfig.getString("username")
      val port = sftpConfig.getIntOption("port").getOrElse(22)

      val creds = sftpConfig.getStringOption("keypair-file").map(Left(_))
        .orElse(sftpConfig.getStringOption("password").map(Right(_)))
        .getOrElse(throw new IllegalArgumentException("Error parsing SFTP URI. No keypair file or password provided."))

      (hostname, username, port, creds)
    }
  }

  def apply(path: String, credentialsConfig: Config): SftpFileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))

  def apply(path: String, credentials: Option[(String, String, Int, Either[String, String])]): SftpFileDescriptor = {
    val creds = credentials.getOrElse(throw new IllegalArgumentException(s"No credentials found for $path"))
    creds match {
      case (hostname, username, port, Left(keypair)) =>
        apply(path, hostname, username = username, key = keypair, port = port)
      case (hostname, username, port, Right(password)) =>
        apply(path, hostname, username = username, password = password, port = port)
    }
  }

  def apply(path: String, host: String, username: String, key: String, port: Int)(implicit d: DummyImplicit): SftpFileDescriptor =
    apply(path, SSHOptions(host = host, username = username, identities = List(SSHIdentity(key)), port = port))

  def apply(path: String, host: String, username: String, password: String, port: Int)(implicit d1: DummyImplicit, d2: DummyImplicit): SftpFileDescriptor =
    apply(path, SSHOptions(host = host, username = username, password = password, port = port))
}
