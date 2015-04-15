package eu.shiftforward.apso.io

import java.io.{ FileWriter, File }
import java.nio.file.{ Files, Path, Paths }

import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.Implicits.ApsoCloseable

import scala.io.Source
import scala.util.{ Failure, Success, Try }

case class LocalFileDescriptor(initialPath: String) extends FileDescriptor with Logging {

  private def normalizedPath: Path = file.toPath

  lazy val path: String = file.getAbsolutePath

  lazy val name: String = file.getName

  lazy val file = Paths.get(initialPath).normalize().toAbsolutePath.toFile

  def isDirectory: Boolean = Files.isDirectory(normalizedPath)

  def parent(n: Int): LocalFileDescriptor = {
    val parentPath = (1 to n).foldLeft(normalizedPath)((acc, _) => acc.getParent)
    LocalFileDescriptor(parentPath.toString)
  }

  override def /(child: String): LocalFileDescriptor = addChild(child)

  def addChild(child: String): LocalFileDescriptor = {
    val childPath = normalizedPath.resolve(child)
    LocalFileDescriptor(childPath.toString)
  }

  override def addChildren(children: String*): LocalFileDescriptor = {
    val childPath = children.foldLeft(normalizedPath)((acc, child) => acc.resolve(child))
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

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Option[LocalFileDescriptor] = {
    if (localTarget.isDirectory) {
      download(localTarget.addChild(name))
    } else {

      val downloadFile = if (safeDownloading) {
        localTarget.sibling(_ + ".tmp")
      } else {
        localTarget
      }

      localTarget.mkdirs()

      Try(Files.copy(normalizedPath, downloadFile.normalizedPath)) match {
        case Success(_) =>
          if (safeDownloading) downloadFile.rename(localTarget)
          Some(localTarget)

        case Failure(ex) =>
          log.warn("File copy failed ({})", ex.toString)
          None
      }
    }
  }

  def upload(localTarget: LocalFileDescriptor): Option[LocalFileDescriptor] = {
    if (isDirectory) {
      addChild(name).upload(localTarget)
    } else {

      mkdirs()

      Try(Files.copy(localTarget.normalizedPath, normalizedPath)) match {
        case Success(_) => Some(this)
        case Failure(ex) => log.warn("File copy failed ({})", ex.toString); None
      }
    }
  }

  def list(): Iterator[LocalFileDescriptor] = {
    if (isDirectory) {
      file.listFiles.toIterator.map(f => LocalFileDescriptor(f.getAbsolutePath))
    } else {
      Iterator()
    }
  }

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

  /**
   * Creates intermediary directories
   * @return true if the creation of successful, false otherwise.
   */
  def mkdirs(): Boolean = if (isDirectory) file.mkdirs() else file.getParentFile.mkdirs()

  /**
   * Renames the file pointed by the file descriptor
   * @param to the file descriptor to be renamed to
   * @return a Some of the renamed file descriptor if successful, otherwise None.
   */
  def rename(to: LocalFileDescriptor): Option[LocalFileDescriptor] = {
    to.mkdirs()
    if (file.renameTo(new File(to.path))) Some(to) else None
  }

  /**
   * writes the given string `str` to the file pointed by the file descriptor
   * @param str the string to write to the file
   */
  def write(str: String): Unit = new FileWriter(path).use(_.write(str))

  /**
   * Reads the file pointed by the file descriptor and returns it in string format
   * @return the contents of the file in string format
   */
  def readString: String = Source.fromFile(file, "UTF-8").mkString

  override def toString: String = s"file://$path"
}
