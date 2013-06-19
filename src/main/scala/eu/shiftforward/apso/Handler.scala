package eu.shiftforward.apso

/**
 * Object containing helper methods for handling exceptions.
 */
@deprecated("`scala.util.Try` is a better option for handling exceptions", "1.0")
object Handler {
  private[this] def handler[T]: PartialFunction[Throwable, Option[T]] = {
    case _ => None
  }

  @deprecated("`scala.util.Try` is a better option for handling exceptions", "1.0")
  def handle[T](f: => T): Option[T] = try { Some(f) } catch handler[T]
}
