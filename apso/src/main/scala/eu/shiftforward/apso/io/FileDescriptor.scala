package eu.shiftforward.apso.io

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
   * Downloads the file to the given local destination.
   * If the localTarget is a directory, multiple files can be downloaded to that location.
   *
   * @param localTarget the local destination to which this file should be downloaded.
   *             If the location is a directory, the file will be named the same as the original.
   *             Otherwise, the file will be renamed to the name specified in the file descriptor.
   * @return `true` if the download was successful, `false` otherwise.
   */
  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean = false): Option[LocalFileDescriptor]

  /**
   * Uploads a local file to this file's location.
   * If this file descriptor refers to a directory, multiple files can be downloaded to that location.
   *
   * @param localTarget the local file that should be uploaded.
   *             If this remote location is a directory, the local file will be uploaded with the
   *             same name specified in the localTarget.
   *             Otherwise, the file will be renamed to the remote file descriptor name.
   * @return `true` if the upload was successful, `false` otherwise.
   */
  def upload(localTarget: LocalFileDescriptor): Option[FileDescriptor]

  /**
   * Returns true if the fd points to a directory
   * @return true if the fd points to a directory
   */
  def isDirectory: Boolean

  /**
   * Lists the files in the current file descriptor directory
   * @return a iterator of file descriptors
   */
  def list(): Iterator[FileDescriptor]

  /**
   * Lists the files in the current file descriptor directory that match the given prefix
   * @param prefix the prefix to match the filenames
   * @return a iterator of file descriptors
   */
  def listByPrefix(prefix: String): Iterator[FileDescriptor]

  /**
   * Returns the file descriptor `n` node up in the filesystem path.
   * @param n the number of parent nodes to go back.
   * @return the file descriptor `n` node up in the filesystem path.
   */
  def parent(n: Int = 1): FileDescriptor

  /**
   * Adds a new child node to the filesystem path.
   * @param child the node name.
   * @return the new file descriptor with the updated path
   */
  def addChild(child: String): FileDescriptor

  /**
   * Adds a new child node to the filesystem path.
   * @param child the node name.
   * @return the new file descriptor with the updated path
   */
  def /(child: String): FileDescriptor = addChild(child)

  /**
   * Adds multiple new child nodes to the filesystem path.
   * @param children the node names.
   * @return the new file descriptor with the updated path
   */
  def addChildren(children: String*): FileDescriptor = {
    children.foldLeft(this)((acc, c) => acc.addChild(c))
  }

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
      case (acc, segment) => acc.addChild(segment)
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
  def sibling(f: String => String): FileDescriptor = parent().addChild(f(name))

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
  def apply(uri: String): FileDescriptor = protocol(uri) match {
    case ("file", path) => LocalFileDescriptor(path)
    case ("s3", path) => S3FileDescriptor(path)
    case _ => throw new UnsupportedOperationException("Protocol not supported")
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
