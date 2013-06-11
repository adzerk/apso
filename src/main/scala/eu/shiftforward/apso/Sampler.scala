package eu.shiftforward.apso

import scala.math._

trait Sampler[T] {
  def samplingFor(level: Int): Double
  def apply(level: Int): Seq[T] => Seq[T] = { seq =>
    seq.take((seq.length * samplingFor(level)).toInt)
  }
}

case class ExpSampler[T](base: Double = 2.0) extends Sampler[T] {
  def samplingFor(level: Int) = 1.0 / pow(base, level)
}

case class ListSampler[T](list: Double*) extends Sampler[T] {
  def samplingFor(level: Int) = list(level)
}

trait FallbackToMinimum[T] extends Sampler[T] {
  val minSample: Double
  abstract override def samplingFor(level: Int) =
    max(super.samplingFor(level), minSample)
}
