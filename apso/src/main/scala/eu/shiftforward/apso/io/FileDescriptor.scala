package eu.shiftforward.apso.io

import java.io.InputStream

import com.typesafe.config.{ ConfigFactory, Config }

import scala.io.Source

/**
 * A representation of a file stored in an arbitrary location. A descriptor includes logic to
 * copy files to and from a local filesystem, as well as filesystem navigation logic.
 */
trait FileDescriptor {

  /**
   * Returns the unique identifier of this file in its location.
   * @return the unique identifier of this file in its location.
   */
  def path: String

  /**
   * The name of the file associated to the file descriptor.
   * @return the file name.
   */
  def name: String

  /**
   * The size of the file associated to the file descriptor.
   */
  def size: Long

  /**
   * Downloads the file to the given local destination.
   * Both the source and the destination must point to a file.
   *
   * @param localTarget the local destination to which this file should be downloaded.
   *                    The path must be absolute path to the final file.
   * @param safeDownloading downloads the file to filename + ".tmp" and renames it to the original
   *                        name if the download was successful.
   * @return `true` if the download was successful, `false` otherwise.
   */
  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean = false): Boolean

  /**
   * Uploads a local file to this file's location.
   * Both the source and the target must point to a file.
   *
   * @param localTarget the local file that should be uploaded.
   *             The path must be absolute path to the final file.
   * @return `true` if the upload was successful, `false` otherwise.
   */
  def upload(localTarget: LocalFileDescriptor): Boolean

  /**
   * Returns an input stream for the contents of this file.
   * @return an input stream for the contents of this file.
   */
  def stream(): InputStream

  /**
   * Returns an iterator with the lines of this file.
   * @return an iterator with the lines of this file.
   */
  def lines(): Iterator[String] = Source.fromInputStream(stream()).getLines()

  /**
   * Returns true if the fd points to a directory
   * @return true if the fd points to a directory
   */
  def isDirectory: Boolean

  /**
   * Lists the files in the current file descriptor directory
   * @return a iterator of file descriptors
   */
  def list: Iterator[FileDescriptor]

  /**
   * Lists all files down the file hierarchy tree that whose relative path match the given prefix
   * @param prefix the prefix to match given each file relative path to the current directory
   * @return a iterator of file descriptors
   */
  def listAllFilesWithPrefix(prefix: String): Iterator[FileDescriptor]

  /**
   * Returns the file descriptor `n` node up in the filesystem path.
   * @param n the number of parent nodes to go back.
   * @return the file descriptor `n` node up in the filesystem path.
   */
  def parent(n: Int = 1): FileDescriptor

  /**
   * Adds a new child node to the filesystem path.
   * @param name the node name.
   * @return the new file descriptor with the updated path
   */
  def child(name: String): FileDescriptor

  /**
   * Adds a new child node to the filesystem path.
   * @param name the node name.
   * @return the new file descriptor with the updated path
   */
  def /(name: String): FileDescriptor = child(name)

  /**
   * Adds multiple new child nodes to the filesystem path.
   * @param names the node names to add.
   * @return the new file descriptor with the updated path
   */
  def children(names: String*): FileDescriptor =
    names.foldLeft(this)((acc, c) => acc.child(c))

  /**
   * Changes the path of the file descriptor using unix's cd syntax related to the current directory.
   * Ex: Go back to directories: ../..
   *     Go to child directories: foo/bar
   *     Go to sibling directory: ../foo
   * Absolute path changes are not supported.
   * @param pathString the cd command
   * @return the new file descriptor with the updated path
   */
  def cd(pathString: String): FileDescriptor = {
    pathString.split("/").toList.foldLeft(this) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.parent()
      case (acc, segment) => acc.child(segment)
    }
  }

  /**
   * Returns a new file descriptor pointing to a sibling of the current file descriptor
   * @param name the file name of the new file descriptor
   * @return a new file descriptor pointing to a sibling of the current file descriptor
   */
  def sibling(name: String): FileDescriptor = sibling(_ => name)

  /**
   * Returns a new file descriptor pointing to a sibling of the current file descriptor
   * @param f a function that returns a new name from the current name of the file descriptor
   * @return a new file descriptor pointing to a sibling of the current file descriptor
   */
  def sibling(f: String => String): FileDescriptor = parent().child(f(name))

  /**
   * Returns true if the file pointed by the file descriptor exists
   * @return true if the file pointed by the file descriptor exists
   */
  def exists: Boolean

  /**
   * Deletes the file associated to the file descriptor
   * @return `true` if the delete was successful, `false` otherwise.
   */
  def delete(): Boolean

  /**
   * Creates intermediary directories
   * @return true if the creation of successful, false otherwise.
   */
  def mkdirs(): Boolean
}

object FileDescriptor {

  /**
   * Creates the specific File descriptor from a URI
   * @param uri the URI
   * @return the specific file descriptor given the supported protocols
   */
  def apply(uri: String): FileDescriptor = apply(uri, ConfigFactory.empty)

  def apply(uri: String, credentialsConfig: Config): FileDescriptor = {
    protocol(uri) match {
      case ("file", path) => LocalFileDescriptor(path)
      case ("s3", path) => S3FileDescriptor(path, credentialsConfig)
      case ("sftp", path) => SftpFileDescriptor(path, credentialsConfig)
      case _ => throw new UnsupportedOperationException("Protocol not supported")
    }
  }

  /**
   * Splits the protocol from the path for a given URI
   * @param uri the URI
   * @return a tuple of (protocol, path)
   */
  private def protocol(uri: String) = uri.split("://").toList match {
    case protocol :: path :: Nil => (protocol, path)
    case _ => throw new IllegalArgumentException("Malformed URI")
  }
}
