package com.velocidi.apso.io

import java.io.InputStream

/** Provides the same functionality as `java.io.SequenceInputStream`, i.e. a
  * `SequenceInputStream` represents the logical concatenation of other input streams.
  * Unlike `SequenceInputStream`, this implementation will open the concatenated input
  * streams lazily (as needed). At any point while reading from the stream, only one
  * underlying `InputStream` should be open.
  * @param streams A sequence of `InputStream` thunks
  */
class LazySequenceInputStream(private[this] var streams: Seq[() => InputStream]) extends InputStream {
  private[this] var current: InputStream = null

  private[this] def nextStream() = {
    if (current != null) current.close()

    if (streams.nonEmpty) {
      current = streams.head()
      streams = streams.tail

    } else current = null
  }

  nextStream()

  override def available(): Int =
    if (current == null) 0
    else current.available()

  def read(): Int =
    if (current == null) -1
    else {
      val c = current.read()

      if (c == -1) {
        nextStream()
        read()

      } else c
    }

  override def read(b: Array[Byte], off: Int, len: Int): Int =
    if (current == null) -1
    else if (b == null) throw new NullPointerException()
    else if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException()
    else if (len == 0) 0
    else {
      val n = current.read(b, off, len)

      if (n <= 0) {
        nextStream()
        read(b, off, len)
      } else n
    }

  override def close() = if (current != null) current.close()
}
