package com.kevel.apso.hashing

import com.twmacinta.util.MD5

/** Object containing implicit classes and methods related to hashing functions.
  */
object Implicits {

  /** Implicit class that provides new hashing methods for strings.
    * @param s
    *   the string to which the new hashing methods are provided.
    */
  final implicit class ApsoHashingString(val s: String) extends AnyVal {

    /** Returns the MD5 of this string.
      * @return
      *   the MD5 of this string.
      */
    def md5: String = new MD5(s).asHex

    /** Returns the MurmurHash3 of this string.
      * @return
      *   the MurmurHash3 of this string.
      */
    def murmurHash: Long = MurmurHash3.MurmurHash3_x64_64(s.getBytes, 9001)
  }
}
