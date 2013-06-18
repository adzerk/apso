package eu.shiftforward.apso

import scala.compat.Platform._

/**
 * A widget for printing a dynamic progress bar in a console.
 * @param total the number representing the full progress bar
 */
case class ProgressBar(total: Int = 100) {
  private val workchars = List('|', '/', '-', '\\')
  private val format = "\33[2K\r%3d%% %s %c"
  private val progress = new StringBuilder(80)

  private var done = 0
  private var lastTimestamp = currentTime
  private var lastDone = 0

  /**
   * Increase the progress by one.
   */
  def tick() {
    tick(1)
  }

  /**
   * Increase the progress by the given number of units.
   * @param inc the number of progress units to increase
   */
  def tick(inc: Int) {
    if (!isFinished) {
      done += inc

      val currentTimestamp = currentTime
      val throughput =
        (done - lastDone).toDouble / (currentTimestamp - lastTimestamp) * 1000

      val percent = done * 100 / total
      var extrachars = percent / 2 - progress.length

      while (extrachars > 0) {
        extrachars -= 1
        progress.append('#')
      }

      printf(format, percent, progress, workchars(done % workchars.length))
      print(" [" + throughput + "] ops/second")

      lastTimestamp = currentTimestamp
      lastDone = done

      if (isFinished) {
        System.out.flush()
        println()
      }
    }
  }

  /**
   * Returns `true` if this progress bar is full.
   * @return `true` if this progress bar is full, `false` otherwise.
   */
  def isFinished = done >= total
}
