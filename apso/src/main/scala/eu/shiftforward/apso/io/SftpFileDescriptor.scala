package eu.shiftforward.apso.io

import com.jcraft.jsch.JSchException
import eu.shiftforward.apso.Logging
import fr.janalyse.ssh._
import java.io.File

case class SftpFileDescriptor(
    private val sshOptions: SSHOptions,
    private val elements: List[String]) extends FileDescriptor with Logging {

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

  lazy val name: String = elements.lastOption.getOrElse("") // Same as S3FileDescriptor
  lazy val path: String = "/" + elements.mkString("/") // SAME as S3FileDescriptor

  def exists: Boolean = ssh(_.exists(path))
  def isDirectory: Boolean = ssh(_.isDirectory(path))

  def list: Iterator[SftpFileDescriptor] =
    if (isDirectory) {
      ssh(_.ls(path)).toIterator.map(this.child)
    } else {
      Iterator()
    }

  def listAllFilesWithPrefix(prefix: String): Iterator[SftpFileDescriptor] = {
    def aux(f: SftpFileDescriptor): Iterator[SftpFileDescriptor] =
      if (f.isDirectory) f.list.flatMap(aux)
      else Iterator(f)

    this.list.filter(_.name.startsWith(prefix)).flatMap(aux)
  }

  private def sanitize(segment: String): Option[String] = { // SAME as S3FileDescriptor
    val whiteSpaceValidated = segment.trim match {
      case "" => None
      case str => Some(str)
    }

    whiteSpaceValidated.map {
      _.count(_ == '/') match {
        case 0 => segment
        case 1 if segment.endsWith("/") => segment.dropRight(1)
        case _ => throw new IllegalArgumentException("path cannot contain /")
      }
    }
  }

  def parent(n: Int): SftpFileDescriptor = // SAME as S3FileDescriptor
    this.copy(elements = elements.dropRight(n))

  def child(name: String): SftpFileDescriptor = // SAME as S3FileDescriptor
    this.copy(elements = elements ++ sanitize(name).toList)

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
  def apply(host: String, username: String, password: String, port: Int = 2222)(path: String): SftpFileDescriptor = {
    val sshSettings =
      SSHOptions(host = host, username = username, password = password, port = port)

    path.split("/").toList match {
      case Nil => SftpFileDescriptor(sshSettings, Nil)
      case "" :: hd :: tail => SftpFileDescriptor(sshSettings, hd :: tail)
      case _ =>
        throw new IllegalArgumentException("Error parsing SFTP URI. Only absolute paths are supported.")
    }
  }
}
