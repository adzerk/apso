package eu.shiftforward.apso

/**
 * Utility object for measuring the running time of a block of code.
 */
object Benchmark {

  /**
   * Runs a block of code and prints its running time to stdout.
   * @param name a name that identifies this benchmark
   * @param printer the printer to use for displaying the benchmark result. It defaults to `println`
   * @param block the block of code to run
   * @tparam T the return type of the block
   * @return the value returned by the code block.
   */
  def apply[T](name: String, printer: String => Unit = println)(block: => T) = {
    val start = System.currentTimeMillis
    try {
      block
    } finally {
      val diff = System.currentTimeMillis - start
      printer("# Block \"" + name + "\" completed, time taken: " + diff + " ms (" + diff / 1000.0 + " s)")
    }
  }
}
