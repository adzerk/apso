package eu.shiftforward.apso.io

trait FileDescriptor {
  def path: String

  def download(dest: LocalFileDescriptor): Boolean
  def upload(src: LocalFileDescriptor): Boolean

  def parent(n: Int = 1): FileDescriptor
  def addChild(child: String): FileDescriptor
  def addChildren(child: List[String]): FileDescriptor
  def cd(pathString: String): FileDescriptor
}

object FileDescriptor {
  def apply(uri: String): FileDescriptor = protocol(uri) match {
    case ("file", path) => LocalFileDescriptor(path)
    case ("s3", path) => S3FileDescriptor(path)
    case _ => throw new Exception("Protocol not supported")
  }

  private def protocol(uri: String) = uri.split("://").toList match {
    case protocol :: path :: Nil => (protocol, path)
    case _ => throw new Exception("Malformed URI")
  }
}
