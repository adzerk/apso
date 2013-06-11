package eu.shiftforward.apso.hashing

import com.twmacinta.util.MD5

object Implicits {

  final implicit class PimpedString(val s: String) extends AnyVal {
    def md5: String = new MD5(s).asHex
    def murmurHash: Long = MurmurHash3.MurmurHash3_x64_64(s.getBytes, 9001)
  }
}
