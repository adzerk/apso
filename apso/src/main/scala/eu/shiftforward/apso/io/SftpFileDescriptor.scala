package eu.shiftforward.apso.io

import com.typesafe.config.Config
import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.config.FileDescriptorCredentials
import eu.shiftforward.apso.config.Implicits._
import java.io.File
import org.apache.commons.vfs2._
import org.apache.commons.vfs2.impl.StandardFileSystemManager
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import scala.util.{ Properties, Try }

/**
 * A `FileDescriptor` for files served over SFTP. This file descriptor only supports absolute paths.
 * Currently SSH connections are not "cached" and each request requires starting a new connection.
 *
 * The URI for this `FileDescriptor` should be in the format:
 * - `sftp://<username>@<hostname>:<port>/<absolute-path>`
 *
 * Both the username and port are optional. Additionally, the credentials config expects an object
 * with the following format:
 *
 * `sftp {
 *    default = {
 *      username = <username>
 *      password = <password>
 *    }
 *  }`
 *
 * What is considered as an `id` for credentials handling is the `hostname` of the file descriptor,
 * therefore it is possible to provide credentials for a specific `hostname`.
 */
case class SftpFileDescriptor(
  host: String,
  port: Int,
  username: String,
  password: Option[String],
  elements: List[String],
  identities: Array[File] = Array.empty)
    extends FileDescriptor with RemoteFileDescriptor with Logging {
  type Self = SftpFileDescriptor

  @transient private[this] var _fsOpts: FileSystemOptions = _

  private[this] def fsOpts = {
    if (_fsOpts == null) {
      _fsOpts = new FileSystemOptions()

      SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(_fsOpts, "no")
      SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(_fsOpts, false)
      SftpFileSystemConfigBuilder.getInstance().setTimeout(_fsOpts, 10000)
      SftpFileSystemConfigBuilder.getInstance().setIdentities(_fsOpts, identities)
    }

    _fsOpts
  }

  protected[this] val root = ""

  private[this] val remotePath = password match {
    case None => s"sftp://$username@$host$path"
    case Some(pw) => s"sftp://$username:$pw@$host$path"
  }

  private def ssh[A](block: StandardFileSystemManager => A): A = {
    def doConnect(retries: Int): A = {
      val fsManager = new StandardFileSystemManager()
      fsManager.init()
      try {
        block(fsManager)
      } catch {
        case e: FileSystemException if retries > 0 =>
          log.warn("[{}] {}. Retrying in 10 seconds...", host, e.getMessage, null)
          Thread.sleep(10000)
          doConnect(retries - 1)
      } finally {
        fsManager.close()
      }
    }

    doConnect(3)
  }

  protected def duplicate(elements: List[String]) =
    this.copy(elements = elements)

  def exists: Boolean = ssh(_.resolveFile(remotePath, fsOpts).exists())
  def isDirectory: Boolean = ssh(_.resolveFile(remotePath, fsOpts).getType == FileType.FOLDER)

  def list: Iterator[SftpFileDescriptor] =
    if (isDirectory) {
      ssh(_.resolveFile(remotePath, fsOpts).getChildren).toIterator.map(_.getName.getBaseName).map(this.child)
    } else {
      Iterator()
    }

  def listAllFilesWithPrefix(prefix: String): Iterator[SftpFileDescriptor] = {
    def aux(f: SftpFileDescriptor): Iterator[SftpFileDescriptor] =
      if (f.isDirectory) f.list.flatMap(aux)
      else Iterator(f)

    this.list.filter(_.name.startsWith(prefix)).flatMap(aux)
  }

  def delete(): Boolean = Try(ssh(_.resolveFile(remotePath, fsOpts).delete(Selectors.SELECT_ALL))).isSuccess

  def mkdirs(): Boolean = Try(ssh(_.resolveFile(remotePath, fsOpts).createFolder())).isSuccess

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    require(!localTarget.isDirectory, s"File descriptor can't point to a directory: ${localTarget.path}")
    require(!isDirectory, s"File descriptor can't point to a directory: ${this.path}")

    if (localTarget.parent().mkdirs()) {
      val downloadFile = if (safeDownloading) localTarget.sibling(_ + ".tmp") else localTarget

      val downloadResult = Try {
        ssh(m => m.resolveFile(downloadFile.path).copyFrom(m.resolveFile(remotePath, fsOpts), Selectors.SELECT_SELF))
      }

      if (downloadResult.isSuccess && safeDownloading)
        downloadFile.rename(localTarget)

      downloadResult.isSuccess
    } else false
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    require(!localTarget.isDirectory, s"File descriptor can't point to a directory: ${localTarget.path}")
    require(!isDirectory, s"File descriptor can't point to a directory: ${this.path}")

    parent().mkdirs() &&
      Try {
        ssh(m => m.resolveFile(remotePath, fsOpts).copyFrom(m.resolveFile(localTarget.path), Selectors.SELECT_SELF))
      }.isSuccess
  }

  override def toString: String =
    if (port != 22) s"sftp://$username@$host:$port$path"
    else s"sftp://$username@$host$path"

  override def equals(other: Any): Boolean = other match {
    case that: SftpFileDescriptor =>
      username == that.username &&
        host == that.host &&
        port == that.port &&
        username == that.username &&
        password == that.password &&
        (identities sameElements that.identities) &&
        path == that.path
    case _ => false
  }
}

object SftpFileDescriptor {
  private[this] val idRegex = """(.*@)?([\-|\d|\w|\.]+)(:\d+)?(\/.*)""".r

  private def splitMeta(url: String): (String, Int, String) = {
    url match {
      case idRegex(_, id, null, path) => (id, 22, path)
      case idRegex(_, id, port, path) => (id, port.drop(1).toInt, path)
      case _ => throw new IllegalArgumentException("Error parsing SFTP URI.")
    }
  }

  private[this] val credentials = new FileDescriptorCredentials[(String, String, Either[String, String])] {
    final val protocol = "sftp"
    def id(path: String) = splitMeta(path)._1

    def createCredentials(hostname: String, sftpConfig: Config): (String, String, Either[String, String]) = {
      val username = sftpConfig.getString("username")

      val creds = sftpConfig.getStringOption("keypair-file").map(Left(_))
        .orElse(sftpConfig.getStringOption("password").map(Right(_)))
        .getOrElse(throw new IllegalArgumentException("Error parsing SFTP URI. No keypair file or password provided."))

      (hostname, username, creds)
    }
  }

  def apply(host: String, port: Int, url: String, username: String, password: Option[String], identities: Array[File])(
    implicit d: DummyImplicit): SftpFileDescriptor = {
    val (_, _, path) = splitMeta(url)

    val elements =
      path.split("/").toList match {
        case Nil => Nil
        case "" :: hd :: tail => hd :: tail
        case _ => throw new IllegalArgumentException("Error parsing SFTP URI. Only absolute paths are supported.")
      }

    SftpFileDescriptor(host, port, username, password, elements, identities)
  }

  def apply(host: String, port: Int, url: String, username: String, password: Option[String])(
    implicit d: DummyImplicit): SftpFileDescriptor =
    apply(host, port, url, username, password, Array[File]())

  def apply(url: String, credentials: Option[(String, String, Either[String, String])]): SftpFileDescriptor = {
    val creds = credentials.getOrElse(throw new IllegalArgumentException(s"No credentials found."))
    val (_, port, _) = splitMeta(url)

    creds match {
      case (host, username, Left(keyPair)) =>
        apply(host, port, url, username, None, Array(new File(Properties.userHome + "/.ssh/" + keyPair)))
      case (host, username, Right(password)) =>
        apply(host, port, url, username, Some(password), Array[File]())
    }
  }

  def apply(path: String, credentialsConfig: Config): SftpFileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))
}
