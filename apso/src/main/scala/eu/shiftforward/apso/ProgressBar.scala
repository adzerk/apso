package eu.shiftforward.apso

import scala.compat.Platform._

/**
 * A widget for printing a dynamic progress bar in a console.
 * @param total the number representing the full progress bar
 * @param width the line width of the progress bar
 * @param throughputUnit the throughput unit that is being measured
 * @param throughputTransformer a function to transform the measured throughput
 *                              value, before displaying it
 */
case class ProgressBar(
    total: Long = 100,
    width: Int = 80,
    throughputUnit: String = "ops",
    throughputTransformer: Double => Double = identity) {
  private[this] var done = 0L
  private[this] val startTimestamp = currentTime

  private[this] val bar = "=" * width
  private[this] val spaces = " " * width

  private[this] val workchars = List('|', '/', '-', '\\')
  private[this] var lastChar = 0
  private[this] def workChar = {
    if (lastChar >= (workchars.length - 1)) lastChar = 0
    else lastChar += 1
    workchars(lastChar)
  }

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
  def tick(inc: Long) {
    if (!isFinished) {
      done += inc
      if (done > total) done = total

      val currentTimestamp = currentTime
      val throughput =
        done.toDouble / (currentTimestamp - startTimestamp) * 1000

      val percent = done.toDouble / total
      val res = new StringBuilder(width)

      res.append("\r")
      res.append(f"${(percent * 100).toInt}%3d%% ")
      res.append("[")
      res.append(bar.substring(0, (width * percent).toInt))
      if (percent < 1.0) res.append(">")
      res.append(spaces.substring((width * percent).toInt))
      res.append("]")
      res.append(f" [ ${throughputTransformer(throughput)}%2.2f ] $throughputUnit/s")
      res.append("  ")
      print(res.mkString)

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
