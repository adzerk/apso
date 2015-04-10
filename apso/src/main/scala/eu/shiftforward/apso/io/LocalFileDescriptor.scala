package eu.shiftforward.apso.io

import java.io.File
import java.nio.file.{ Files, Path, Paths }

import eu.shiftforward.apso.Logging

import scala.util.{ Failure, Success, Try }

case class LocalFileDescriptor(initialPath: String) extends FileDescriptor with Logging {

  val normalizedPath: Path = Paths.get(initialPath).normalize().toAbsolutePath

  val path: String = normalizedPath.toString

  def isDirectory: Boolean = Files.isDirectory(normalizedPath)

  def parent(n: Int): LocalFileDescriptor = {
    val parentPath = (1 to n).foldLeft(normalizedPath)((acc, _) => acc.getParent)
    LocalFileDescriptor(parentPath.toString)
  }

  def addChild(child: String): LocalFileDescriptor = {
    val childPath = normalizedPath.resolve(child)
    LocalFileDescriptor(childPath.toString)
  }

  def addChildren(children: List[String]): LocalFileDescriptor = {
    val childPath = children.foldLeft(normalizedPath)((acc, child) => acc.resolve(child))
    LocalFileDescriptor(childPath.toString)
  }

  def cd(pathString: String): LocalFileDescriptor = {
    val newPath = pathString.split("/").toList.foldLeft(normalizedPath) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.getParent
      case (acc, segment) => acc.resolve(segment)
    }
    LocalFileDescriptor(newPath.toString)
  }

  def download(localTarget: LocalFileDescriptor): Boolean = {
    val targetPath = if (localTarget.isDirectory) {
      localTarget.addChild(normalizedPath.getFileName.toString).normalizedPath
    } else {
      localTarget.normalizedPath
    }

    Try(Files.copy(normalizedPath, targetPath)) match {
      case Success(_) => true
      case Failure(ex) => log.warn("File copy failed ({})", ex.toString); false
    }
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    val localPath = if (isDirectory) {
      normalizedPath.resolve(localTarget.normalizedPath.getFileName)
    } else {
      normalizedPath
    }

    Try(Files.copy(localTarget.normalizedPath, localPath)) match {
      case Success(_) => true
      case Failure(ex) => log.warn("File copy failed ({})", ex.toString); false
    }
  }

  def list(): Iterator[LocalFileDescriptor] = {
    if (isDirectory) {
      new File(path).listFiles.toIterator.map(f => LocalFileDescriptor(f.getAbsolutePath))
    } else {
      Iterator()
    }
  }

  def listByPrefix(prefix: String): Iterator[FileDescriptor] = {
    if (isDirectory) {
      new File(path).listFiles.toIterator.filter(_.getName.startsWith(prefix)).map {
        f => LocalFileDescriptor(f.getAbsolutePath)
      }
    } else {
      Iterator()
    }
  }

  def delete(): Boolean = {
    new File(path).delete()
  }

  override def toString: String = s"file://$path"
}
