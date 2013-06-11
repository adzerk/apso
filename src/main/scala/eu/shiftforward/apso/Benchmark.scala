package eu.shiftforward.apso

object Benchmark {
  def apply[T](name: String)(block: => T) {
    val start = System.currentTimeMillis
    try {
      block
    } finally {
      val diff = System.currentTimeMillis - start
      println("# Block \"" + name + "\" completed, time taken: " + diff + " ms (" + diff / 1000.0 + " s)")
    }
  }
}

