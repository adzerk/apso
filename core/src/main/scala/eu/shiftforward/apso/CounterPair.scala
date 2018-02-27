package eu.shiftforward.apso

/**
 * Factory and extractor for packing two numbers in the range of an unsigned
 * short (0 to 65535) in an `Int`.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
object CounterPair {

  /**
   * Packs two small numbers into an `Int`. The two given numbers must be in the
   * range of an unsigned short (0 to 65535) in an `Int`. If they are not, the
   * behavior is undefined.
   * @param first the first number to be packed
   * @param second the second number to be packed
   * @return an integer into which the two numbers are packed.
   */
  @inline def apply(first: Int, second: Int): Int = (second << 16) | first

  /**
   * Unpacks two small numbers previously packed into an `Int`.
   * @param data the integer from which the two numbers are to be unpacked
   * @return the pair of packed numbers wrapped into a `Some`.
   */
  @inline def unapply(data: Int): Option[(Int, Int)] = Some((data & 0xFFFF, data >>> 16))
}
