package eu.shiftforward.apso

/**
  * Constructor and extractor for packing two numbers in an `Int`.
  */
object CounterPair {
  @inline def apply(first: Int, second: Int) = (second << 16) | first
  @inline def unapply(data: Int): Option[(Int, Int)] = Some(data & 0xFFFF, data >>> 16)
}
