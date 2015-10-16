package eu.shiftforward.apso.io

trait RemoteFileDescriptor { this: FileDescriptor =>
  protected def elements: List[String]
  protected def root: String

  val name: String = elements.lastOption.getOrElse("")
  val path: String = root + elements.mkString("/")

  private def sanitize(segment: String): Option[String] = {
    val whiteSpaceValidated = segment.trim match {
      case "" => None
      case str => Some(str)
    }

    whiteSpaceValidated.map {
      _.count(_ == '/') match {
        case 0 => segment
        case 1 if segment.endsWith("/") => segment.dropRight(1)
        case _ => throw new IllegalArgumentException("path cannot contain /")
      }
    }
  }

  protected def copy(elements: List[String]): FileDescriptor

  def parent(n: Int): FileDescriptor =
    this.copy(elements = elements.dropRight(n))

  def child(name: String): FileDescriptor =
    this.copy(elements = elements ++ sanitize(name).toList)

  override def children(names: String*): FileDescriptor =
    this.copy(elements = elements ++ names.flatMap(sanitize))
}
