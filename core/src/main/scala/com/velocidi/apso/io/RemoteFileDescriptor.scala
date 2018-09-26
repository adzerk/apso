package com.velocidi.apso.io

trait RemoteFileDescriptor { this: FileDescriptor =>
  type Self <: RemoteFileDescriptor

  protected def elements: List[String]
  protected def root: String

  lazy val name: String = elements.lastOption.getOrElse("")
  lazy val path: String = root + "/" + elements.mkString("/")

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

  protected def duplicate(elements: List[String]): Self

  def parent(n: Int = 1): Self =
    this.duplicate(elements = elements.dropRight(n))

  def child(name: String): Self =
    this.duplicate(elements = elements ++ sanitize(name).toList)

  override def children(names: String*): Self =
    this.duplicate(elements = elements ++ names.flatMap(sanitize))
}
