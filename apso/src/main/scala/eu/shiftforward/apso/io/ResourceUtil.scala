package eu.shiftforward.apso.io

import java.io.InputStream
import java.net.URLDecoder

import scala.io.Source

/**
  * Utility methods for handling resource files
  */
object ResourceUtil {
  val defaultEncoding = "UTF-8"

  def getResourceURL(path: String, encoding: String = defaultEncoding): String =
    URLDecoder.decode(getClass.getResource("/" + path).getFile, encoding)

  def getResourceStream(path: String): InputStream =
    getClass.getResourceAsStream("/" + path)

  def getResourceAsString(path: String, encoding: String = defaultEncoding) =
    Source.fromInputStream(getResourceStream(path: String), encoding).mkString
}
