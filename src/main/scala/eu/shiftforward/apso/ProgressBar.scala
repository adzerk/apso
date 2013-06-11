package eu.shiftforward.apso

import scala.compat.Platform._

case class ProgressBar(total: Int = 100) {
  val workchars = List('|', '/', '-', '\\')
  val format = "\33[2K\r%3d%% %s %c"
  val progress = new StringBuilder(80)

  var done = 0
  var lastTimestamp = currentTime
  var lastDone = 0

  def tick() {
    tick(1)
  }

  def tick(inc: Int) {
    if (!isFinished) {
      done += inc

      val currentTimestamp = currentTime
      val throughput =
        ((done - lastDone).toDouble / (currentTimestamp - lastTimestamp)) * 1000

      val percent = (done * 100) / total
      var extrachars = (percent / 2) - progress.length

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

  def isFinished = done >= total
}
