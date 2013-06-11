package eu.shiftforward.apso

trait ProgressListener {
  def onStart(tickCount: Int) {}
  def onTick() {}
  def onFinish() {}
}
