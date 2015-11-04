package eu.shiftforward.apso.io

import com.jcraft.jsch.UserInfo
import com.typesafe.config.{ Config, ConfigFactory }
import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.config.FileDescriptorCredentials
import eu.shiftforward.apso.config.Implicits._
import java.io.File
import java.util.concurrent.Semaphore
import org.apache.commons.vfs2._
import org.apache.commons.vfs2.impl.StandardFileSystemManager
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import scala.collection.mutable.HashMap
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
 * Or if using public key authentication:
 * `sftp {
 *    default = {
 *      username = <username>
 *      keypair-file = <key filename>
 *      passphrase = <passphrase>
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
  identity: Option[SftpFileDescriptor.Identity])
    extends FileDescriptor with RemoteFileDescriptor with Logging {

  case class SftpPassphraseUserInfo(passphrase: Option[String]) extends UserInfo {
    def getPassphrase() = passphrase.getOrElse("")
    def getPassword() = ""
    def promptPassphrase(s: String) = true
    def promptPassword(s: String) = true
    def promptYesNo(s: String) = true
    def showMessage(message: String) {}
  }

  type Self = SftpFileDescriptor

  @transient private[this] var _fsOpts: FileSystemOptions = _

  private[this] def fsOpts = {
    if (_fsOpts == null) {
      _fsOpts = new FileSystemOptions()

      SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(_fsOpts, "no")
      SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(_fsOpts, false)
      SftpFileSystemConfigBuilder.getInstance().setTimeout(_fsOpts, 10000)

      identity.foreach {
        case (f, p) =>
          SftpFileSystemConfigBuilder.getInstance().setIdentities(_fsOpts, Array(f))
          SftpFileSystemConfigBuilder.getInstance().setUserInfo(_fsOpts, SftpPassphraseUserInfo(p))
      }
    }

    _fsOpts
  }

  protected[this] val root = ""

  private[this] val remotePath = password match {
    case None => s"sftp://$username@$host:$port$path"
    case Some(pw) => s"sftp://$username:$pw@$host:$port$path"
  }

  private def ssh[A](block: StandardFileSystemManager => A): A = {
    def doConnect(retries: Int): A = {
      try {
        SftpFileDescriptor.acquireConnection(host)

        val fsManager = new StandardFileSystemManager()

        try {
          fsManager.init()
          block(fsManager)

        } finally {
          fsManager.close()
          SftpFileDescriptor.releaseConnection(host)
        }
      } catch {
        case e: FileSystemException if retries > 0 =>
          log.warn("[{}] {}. Retrying in 10 seconds...", host, e.getMessage, null)
          log.debug("Failure cause: {}", e.getCause)
          Thread.sleep(10000)
          doConnect(retries - 1)
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

  def delete(): Boolean = Try(ssh(_.resolveFile(remotePath, fsOpts).delete(Selectors.SELECT_SELF)) > 0).getOrElse(false)

  def mkdirs(): Boolean = Try(ssh(_.resolveFile(remotePath, fsOpts).createFolder())).isSuccess

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    require(!localTarget.isDirectory, s"Local file descriptor can't point to a directory: ${localTarget.path}")
    require(!isDirectory, s"Remote file descriptor can't point to a directory: ${this.path}")

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
    require(!localTarget.isDirectory, s"Local file descriptor can't point to a directory: ${localTarget.path}")
    require(!isDirectory, s"Remote file descriptor can't point to a directory: ${this.path}")

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
        identity == that.identity &&
        path == that.path
    case _ => false
  }
}

object SftpFileDescriptor {
  private[this] val config = ConfigFactory.load()
  private[this] val maxConnections =
    config.getInt("apso.io.file-descriptor.sftp.max-connections-per-host")

  private[this] val currentConnections = HashMap[String, Semaphore]()

  def acquireConnection(host: String) = {
    val semaphore = synchronized {
      currentConnections.getOrElseUpdate(host, new Semaphore(maxConnections, true))
    }
    semaphore.acquire()
  }

  def releaseConnection(host: String) = synchronized {
    currentConnections.get(host).foreach(_.release())
  }

  type Identity = (File, Option[String]) // (key, passphrase)

  private[this] val idRegex = """(.*@)?([\-|\d|\w|\.]+)(:\d+)?(\/.*)""".r

  private def splitMeta(url: String): (String, Int, String) = {
    url match {
      case idRegex(_, id, null, path) => (id, 22, path)
      case idRegex(_, id, port, path) => (id, port.drop(1).toInt, path)
      case _ => throw new IllegalArgumentException("Error parsing SFTP URI.")
    }
  }

  private[this] val credentials = new FileDescriptorCredentials[(String, String, Either[Identity, String])] {
    final val protocol = "sftp"
    def id(path: String) = splitMeta(path)._1

    def createCredentials(hostname: String, sftpConfig: Config): (String, String, Either[Identity, String]) = {
      val username = sftpConfig.getString("username")

      val creds =
        sftpConfig.getStringOption("keypair-file").map { fname =>
          Left((new File(Properties.userHome + "/.ssh/" + fname), sftpConfig.getStringOption("passphrase")))
        }
          .orElse(sftpConfig.getStringOption("password").map(Right(_)))
          .getOrElse(throw new IllegalArgumentException("Error parsing SFTP URI. No keypair file or password provided."))

      (hostname, username, creds)
    }
  }

  def apply(host: String, port: Int, url: String, username: String, password: Option[String], identity: Option[Identity])(
    implicit d: DummyImplicit): SftpFileDescriptor = {
    val (_, _, path) = splitMeta(url)

    val elements =
      path.split("/").toList match {
        case Nil => Nil
        case "" :: hd :: tail => hd :: tail
        case _ => throw new IllegalArgumentException("Error parsing SFTP URI. Only absolute paths are supported.")
      }

    SftpFileDescriptor(host, port, username, password, elements, identity)
  }

  def apply(host: String, port: Int, url: String, username: String, password: Option[String])(
    implicit d: DummyImplicit): SftpFileDescriptor =
    apply(host, port, url, username, password, None)

  def apply(url: String, credentials: Option[(String, String, Either[Identity, String])]): SftpFileDescriptor = {
    val creds = credentials.getOrElse(throw new IllegalArgumentException(s"No credentials found."))
    val (_, port, _) = splitMeta(url)

    creds match {
      case (host, username, Left(identity)) =>
        apply(host, port, url, username, None, Some(identity))
      case (host, username, Right(password)) =>
        apply(host, port, url, username, Some(password), None)
    }
  }

  def apply(path: String, credentialsConfig: Config): SftpFileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))
}
