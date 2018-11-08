package com.velocidi.apso.io

import java.io._
import java.util.concurrent.{ ConcurrentHashMap, TimeoutException }

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{ Properties, Try }

import com.typesafe.config.ConfigFactory
import io.github.andrebeat.pool.{ Lease, Pool }
import net.schmizz.sshj._
import net.schmizz.sshj.common.SSHException
import net.schmizz.sshj.sftp._
import net.schmizz.sshj.transport.verification._
import net.schmizz.sshj.xfer.InMemorySourceFile

import com.velocidi.apso.Logging

/**
 * A `FileDescriptor` for files served over SFTP. This file descriptor only supports absolute paths.
 * The SSH connections for a given host are pooled.
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
    identity: Option[SftpFileDescriptor.Identity],
    @transient private var _fileAttributes: Option[FileAttributes] = None)
  extends FileDescriptor with RemoteFileDescriptor with Logging {

  type Self = SftpFileDescriptor

  def fileAttributes = if (_fileAttributes == null) None else _fileAttributes

  protected[this] val root = ""

  private[this] def sftpClient() =
    SftpFileDescriptor.acquireConnection(host, port, username, password, identity)

  private[this] def sftp[A](block: SFTPClient => A): A = {
    def doConnect(retries: Int): A =
      try {
        sftpClient().use { sftp =>
          block(sftp)
        }
      } catch {
        case e: SFTPException if e.getStatusCode == Response.StatusCode.NO_SUCH_FILE =>
          throw new FileNotFoundException(toString)
        case e @ (_: SSHException | _: IOException) if retries > 0 =>
          log.warn(s"[$host] ${e.getMessage}. Retrying in 10 seconds...")
          log.debug(s"Failure cause: ${e.getCause}")
          Thread.sleep(10000)
          doConnect(retries - 1)
      }

    doConnect(3)
  }

  protected def duplicate(elements: List[String]) =
    this.copy(elements = elements, _fileAttributes = None)

  protected def withFileAttributes[A](f: FileAttributes => A): A = fileAttributes match {
    case Some(fa) => f(fa)
    case None =>
      _fileAttributes = Some(sftp(_.stat(path)))
      withFileAttributes(f)
  }

  def size = withFileAttributes(_.getSize)

  def isDirectory: Boolean = withFileAttributes(_.getType == FileMode.Type.DIRECTORY)

  def exists: Boolean = fileAttributes.isDefined || sftp(c => c.statExistence(path) != null)

  def list: Iterator[SftpFileDescriptor] =
    if (isDirectory) {
      sftp(_.ls(path)).asScala.toIterator.map { r =>
        this.child(r.getName).copy(_fileAttributes = Some(r.getAttributes))
      }
    } else {
      Iterator()
    }

  def listAllFilesWithPrefix(prefix: String): Iterator[SftpFileDescriptor] = {
    val sanitizedPrefix = prefix.dropWhile(_ == '/')
    val prePrefix = sanitizedPrefix.takeWhile(_ != '/')
    val posPrefix = sanitizedPrefix.dropWhile(_ != '/').drop(1)
    this.list.filter(_.name.startsWith(prePrefix)).flatMap { f =>
      if (f.isDirectory) f.listAllFilesWithPrefix(posPrefix)
      else Iterator(f)
    }
  }

  def delete(): Boolean =
    Try {
      if (isDirectory) sftp(_.rmdir(path))
      else sftp(_.rm(path))
    }.isSuccess

  def mkdirs(): Boolean =
    Try(sftp(_.mkdirs(path))).isSuccess

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    require(!localTarget.isDirectory, s"Local file descriptor can't point to a directory: ${localTarget.path}")
    require(!isDirectory, s"Remote file descriptor can't point to a directory: ${this.path}")

    log.info(s"Downloading '$toString' to '$localTarget'")

    if (localTarget.parent().mkdirs()) {
      val downloadFile = if (safeDownloading) localTarget.sibling(_ + ".tmp") else localTarget
      val downloadResult = Try(sftp(_.get(path, downloadFile.path)))

      if (downloadResult.isSuccess && safeDownloading) downloadFile.rename(localTarget)

      downloadResult.isSuccess
    } else false
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    require(!localTarget.isDirectory, s"Local file descriptor can't point to a directory: ${localTarget.path}")
    require(!exists || !isDirectory, s"Remote file descriptor can't point to a directory: ${this.path}")

    log.info(s"Uploading '$localTarget' to '$toString'")

    parent().mkdirs() &&
      Try(sftp(_.put(localTarget.path, path))).isSuccess
  }

  def upload(inputStream: InputStream, length: Option[Long]): Boolean = {
    require(!exists || !isDirectory, s"Remote file descriptor can't point to a directory: ${this.path}")

    log.info(s"Uploading to '$toString'")

    val sourceFile = new InMemorySourceFile {
      override def getLength: Long = length.getOrElse(0)
      override val getName: String = elements.last
      override val getInputStream: InputStream = inputStream
    }

    parent().mkdirs() &&
      Try(sftp(_.put(sourceFile, path))).isSuccess
  }

  def stream(offset: Long = 0L) = new InputStream {
    private[this] lazy val sftpLease = sftpClient()
    private[this] lazy val sftp = sftpLease.get()
    private[this] lazy val remoteFile = sftp.open(path)
    private[this] lazy val inner = new remoteFile.RemoteFileInputStream()
    if (offset > 0) inner.skip(offset)

    def read() = inner.read()

    override def read(b: Array[Byte]) = inner.read(b)
    override def read(b: Array[Byte], off: Int, len: Int): Int = inner.read(b, off, len)
    override def skip(n: Long) = inner.skip(n)
    override def available() = inner.available()
    override def mark(readlimit: Int) = inner.mark(readlimit)
    override def markSupported() = inner.markSupported()
    override def reset() = inner.reset()
    override def close() = {
      inner.close()
      sftpLease.release()
    }
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
  private[this] val fdConf = ConfigFactory.load()
  private[this] val maxConnections = fdConf.getInt("apso.io.file-descriptor.sftp.max-connections-per-host")
  private[this] val maxIdleTime = Duration.fromNanos(
    fdConf.getDuration("apso.io.file-descriptor.sftp.max-idle-time").toNanos)

  private[this] val connectionPools = new ConcurrentHashMap[String, Pool[SftpClient]]()

  // This is just a helper class to tie together the lifetime of the SFTPClient
  // with the SSHClient, i.e. when we close the SFTPClient we also want to
  // disconnect the underlying SSH connection.
  private class SftpClient(private[this] val ssh: SSHClient) {
    val sftpClient = ssh.newSFTPClient
    def close() = {
      sftpClient.close()
      ssh.disconnect()
    }
  }

  implicit def sftpClientToSFTPClient(c: SftpClient): SFTPClient = c.sftpClient

  private[this] def sshClient(
    host: String,
    port: Int,
    username: String,
    password: Option[String],
    identity: Option[Identity]) = {

    val sshClient = new SSHClient()
    sshClient.addHostKeyVerifier(new PromiscuousVerifier())
    sshClient.useCompression()
    sshClient.connect(host, port)

    (password, identity) match {
      case (Some(p), _) =>
        sshClient.authPassword(username, password.get)
      case (_, Some((f, p))) =>
        val keyProvider = p match {
          case Some(passphrase) =>
            sshClient.loadKeys(f.getAbsolutePath, passphrase)
          case None =>
            sshClient.loadKeys(f.getAbsolutePath)
        }

        sshClient.authPublickey(username, keyProvider)
      case _ =>
        throw new IllegalArgumentException("Either a password or pub-priv key identity must be provided")
    }

    sshClient
  }

  private def acquireConnection(
    host: String,
    port: Int,
    username: String,
    password: Option[String],
    identity: Option[Identity]): Lease[SftpClient] = {

    val pool = {
      val p = connectionPools.get(host)
      if (p == null) {
        synchronized {
          val pool = Pool(
            maxConnections,
            () => new SftpClient(sshClient(host, port, username, password, identity)),
            dispose = { c: SftpClient => c.close() },
            maxIdleTime = maxIdleTime)

          connectionPools.put(host, pool)

          pool
        }
      } else p
    }

    pool.tryAcquire(10.seconds) match {
      case Some(lease) => lease
      case None => throw new TimeoutException("Failed to acquire a SFTP client within 10 seconds.")
    }
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

  private[this] val credentials = new FileDescriptorCredentials[config.Credentials.Sftp.Entry, (String, String, Either[Identity, String])] {
    def id(path: String) = splitMeta(path)._1

    def createCredentials(hostname: String, sftpConfig: config.Credentials.Sftp.Entry): (String, String, Either[Identity, String]) = {
      sftpConfig match {
        case config.Credentials.Sftp.Entry.Basic(username, password) =>
          (hostname, username, Right(password))
        case config.Credentials.Sftp.Entry.PublicKey(username, keypairFile, passphrase) =>
          (hostname, username, Left((new File(Properties.userHome + "/.ssh/" + keypairFile), passphrase)))
      }
    }
  }

  def apply(
    host: String,
    port: Int,
    url: String,
    username: String,
    password: Option[String],
    identity: Option[Identity])(implicit d: DummyImplicit): SftpFileDescriptor = {

    val (_, _, path) = splitMeta(url)

    val elements =
      path.split("/").toList match {
        case Nil => Nil
        case "" :: hd :: tail => hd :: tail
        case _ => throw new IllegalArgumentException("Error parsing SFTP URI. Only absolute paths are supported.")
      }

    SftpFileDescriptor(host, port, username, password, elements, identity)
  }

  def apply(
    host: String,
    port: Int,
    url: String,
    username: String,
    password: Option[String])(implicit d: DummyImplicit): SftpFileDescriptor = {

    apply(host, port, url, username, password, None)
  }

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

  def apply(path: String, credentialsConfig: config.Credentials.Sftp): SftpFileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))
}
