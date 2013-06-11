package eu.shiftforward.apso

object CounterPair {
  @inline def apply(first: Int, second: Int) = (second << 16) | first
  @inline def unapply(data: Int): Option[(Int, Int)] = Some(data & 0xFFFF, data >>> 16)
}
