package eu.shiftforward.apso

import java.net.URLDecoder
import java.io.InputStream

trait TestHelper {
  def getResourceURL(resource: String): String =
    URLDecoder.decode(getClass.getResource(resource).getFile, "UTF-8")

  def getResourceStream(resource: String): InputStream =
    getClass.getResourceAsStream(resource)
}
