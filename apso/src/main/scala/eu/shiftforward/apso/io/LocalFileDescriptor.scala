package eu.shiftforward.apso.io

import java.io.{ FileWriter, File }
import java.nio.file.{ StandardCopyOption, Files, Path, Paths }

import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.Implicits.ApsoCloseable

import scala.io.Source
import scala.util.{ Failure, Success, Try }

case class LocalFileDescriptor(initialPath: String) extends FileDescriptor with Logging {

  private def normalizedPath: Path = file.toPath

  lazy val path: String = file.getAbsolutePath

  lazy val name: String = file.getName

  lazy val file = Paths.get(initialPath).normalize().toAbsolutePath.toFile

  def isDirectory: Boolean = file.isDirectory

  def parent(n: Int): LocalFileDescriptor = {
    val parentPath = (1 to n).foldLeft(normalizedPath)((acc, _) => acc.getParent)
    LocalFileDescriptor(parentPath.toString)
  }

  override def /(name: String): LocalFileDescriptor = child(name)

  def child(name: String): LocalFileDescriptor = {
    val childPath = normalizedPath.resolve(name)
    LocalFileDescriptor(childPath.toString)
  }

  override def child(name: String, name2: String, names: String*): LocalFileDescriptor = {
    val childPath = names.foldLeft(child(name).child(name2).normalizedPath) {
      (acc, child) => acc.resolve(child)
    }
    LocalFileDescriptor(childPath.toString)
  }

  override def cd(pathString: String): LocalFileDescriptor = {
    val newPath = pathString.split("/").map(_.trim).toList.foldLeft(normalizedPath) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.getParent
      case (acc, segment) => acc.resolve(segment)
    }
    LocalFileDescriptor(newPath.toString)
  }

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    if (isDirectory || localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {

      val downloadFile = if (safeDownloading) localTarget.sibling(_ + ".tmp")
      else localTarget

      localTarget.parent().mkdirs()

      val result = Try(Files.copy(normalizedPath, downloadFile.normalizedPath,
        StandardCopyOption.REPLACE_EXISTING))

      result match {
        case Success(_) => if (safeDownloading) downloadFile.rename(localTarget)
        case Failure(ex) => log.warn("File copy failed ({})", ex.toString)
      }

      result.isSuccess
    }
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    if (isDirectory || localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {

      parent().mkdirs()

      val result = Try(Files.copy(localTarget.normalizedPath, normalizedPath,
        StandardCopyOption.REPLACE_EXISTING))

      result match {
        case Success(_) =>
        case Failure(ex) => log.warn("File copy failed ({})", ex.toString)
      }

      result.isSuccess
    }
  }

  override def list: Iterator[LocalFileDescriptor] = listByPrefix("")

  def listByPrefix(prefix: String): Iterator[LocalFileDescriptor] = {
    if (isDirectory) {
      file.listFiles.toIterator.filter(_.getName.startsWith(prefix)).map {
        f => LocalFileDescriptor(f.getAbsolutePath)
      }
    } else {
      Iterator()
    }
  }

  def exists: Boolean = file.exists()

  override def sibling(f: String => String): LocalFileDescriptor = {
    LocalFileDescriptor(normalizedPath.resolveSibling(f(name)).toString)
  }

  def delete(): Boolean = file.delete()

  def mkdirs(): Boolean = exists || file.mkdirs()

  /**
   * Renames the file pointed by the file descriptor
   * @param to the file descriptor to be renamed to
   * @return a Some of the renamed file descriptor if successful, otherwise None.
   */
  def rename(to: LocalFileDescriptor): Option[LocalFileDescriptor] = {
    to.mkdirs()
    if (file.renameTo(to.file)) Some(to) else None
  }

  /**
   * writes the given string `str` to the file pointed by the file descriptor
   * @param str the string to write to the file
   */
  def write(str: String): Unit = {
    parent().mkdirs()
    new FileWriter(path).use(_.write(str))
  }

  /**
   * Reads the file pointed by the file descriptor and returns it in string format
   * @return the contents of the file in string format
   */
  def readString: String = Source.fromFile(file, "UTF-8").mkString

  override def toString: String = s"file://$path"

  override def equals(other: Any): Boolean = other match {
    case that: LocalFileDescriptor => path == that.path
    case _ => false
  }
}
