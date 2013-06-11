package eu.shiftforward.apso

object Handler {
  private def handler[T]: PartialFunction[Throwable, Option[T]] = {
    case _ => None
  }

  def handle[T](f: => T): Option[T] = try { Some(f) } catch handler[T]
}
