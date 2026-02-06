package com.kevel.apso

import JreVersionHelper.*

trait JreVersionHelper {
  def jreVersion: (Int, Int) = System.getProperty("java.version") match {
    case VersionRegex(ma, mi) => (ma.toInt, mi.toInt)
    case v                    => throw new Exception(s"Cannot parse JRE version: $v")
  }
}

object JreVersionHelper extends JreVersionHelper {
  val VersionRegex = """^([\d]+)\.([\d]+).*$""".r
}
