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
   * Downloads the file to the given local destination.
   * If the localTarget is a directory, multiple files can be downloaded to that location.
   *
   * @param localTarget the local destination to which this file should be downloaded.
   *             If the location is a directory, the file will be named the same as the original.
   *             Otherwise, the file will be renamed to the name specified in the file descriptor.
   * @return `true` if the download was successful, `false` otherwise.
   */
  def download(localTarget: LocalFileDescriptor): Boolean

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
  def upload(localTarget: LocalFileDescriptor): Boolean

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
   * Adds multiple new child nodes to the filesystem path.
   * @param children the node names.
   * @return the new file descriptor with the updated path
   */
  def addChildren(children: List[String]): FileDescriptor

  /**
   * Changes the path of the file descriptor using unix's cd syntax related to the current directory.
   * Ex: Go back to directories: ../..
   *     Go to child directories: foo/bar
   *     Go to sibling directory: ../foo
   * Absolute path changes are not supported.
   * @param pathString the cd command
   * @return the new file descriptor with the updated path
   */
  def cd(pathString: String): FileDescriptor
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
    case _ => throw new Exception("Protocol not supported")
  }

  /**
   * Splits the protocol from the path for a given URI
   * @param uri the URI
   * @return a tuple of (protocol, path)
   */
  private def protocol(uri: String) = uri.split("://").toList match {
    case protocol :: path :: Nil => (protocol, path)
    case _ => throw new Exception("Malformed URI")
  }
}
