package eu.shiftforward.apso

import java.net.URLDecoder
import java.io.{ File, InputStream }

trait TestHelper {

  def createTempDir(prefix: String = "logs", suffix: String = ""): File = {
    val temp = File.createTempFile(prefix, suffix)
    temp.delete()
    temp.mkdir()
    temp.deleteOnExit()
    temp
  }

  @deprecated("Use eu.shiftforward.apso.io.getResourceURL instead where the `resource` argument " +
    "is already prefixed with a '/'")
  def getResourceURL(resource: String): String =
    URLDecoder.decode(getClass.getResource(resource).getFile, "UTF-8")

  @deprecated("Use eu.shiftforward.apso.io.getResourceStream instead where the `resource` " +
    "argument is already prefixed with a '/'")
  def getResourceStream(resource: String): InputStream =
    getClass.getResourceAsStream(resource)
}
