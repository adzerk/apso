package eu.shiftforward.apso

/**
 * Generic trait for listeners that handle the progress of a process. A process is
 * conceptualized as a sequence of discrete steps (ticks) that notify listeners between
 * each one.
 */
trait ProgressListener {

  /**
   * Handles the start of the process.
   * @param tickCount the number of ticks of this process
   */
  def onStart(tickCount: Int) {}

  /**
   * Handles a tick of the process.
   */
  def onTick() {}

  /**
   * Handles the end of the process.
   */
  def onFinish() {}
}
