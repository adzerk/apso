package com.velocidi.apso

import scala.math._

/**
 * Class that encapsulates a sampling strategy over a sequence of elements.
 * @tparam T the type of the elements of the sequence
 * @todo move the type parameter T to the apply method.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
trait Sampler[T] {

  /**
   * Returns the ratio of elements that correspond to the given sampling level.
   * @param level the level of sampling to use
   * @return the ratio of elements that correspond to the given sampling level,
   *         in the range [0.0, 1.0].
   */
  def samplingFor(level: Int): Double

  /**
   * Creates a sampling function capable of extracting a sample from any
   * sequence.
   * @param level the level of sampling to use
   * @return a function capable of extracting a sample from any sequence.
   */
  def apply(level: Int): Seq[T] => Seq[T] = { seq =>
    seq.take((seq.length * samplingFor(level)).toInt)
  }
}

/**
 * Sampler in which sampling level ratios are distributed in an exponential
 * way. More formally, each sampling level corresponds to a sample with `1.0 /
 * pow(base, level) * 100%` of the original size.
 * @param base the base to use for calculating the ratios, as described above
 * @tparam T the type of the elements of the sequence
 */
@deprecated("This will be removed in a future version", "2017/07/13")
case class ExpSampler[T](base: Double = 2.0) extends Sampler[T] {
  def samplingFor(level: Int) = 1.0 / pow(base, level)
}

/**
 * Sampler in which ratios for each sampling level are given explicitly as a
 * list.
 * @param list the sampling ratios to use in which sampling level
 * @tparam T the type of the elements of the sequence
 */
@deprecated("This will be removed in a future version", "2017/07/13")
case class ListSampler[T](list: Double*) extends Sampler[T] {
  def samplingFor(level: Int) = list(level)
}

/**
 * Mixin that modifies an existing sampler by setting a minimum ratio for any
 * sampling level. Mixing this trait in, levels which previously corresponded to
 * a sampling ratio below the minimum defined use that minimum.
 * @tparam T the type of the elements of the sequence
 */
@deprecated("This will be removed in a future version", "2017/07/13")
trait FallbackToMinimum[T] extends Sampler[T] {

  /**
   * The minimum sampling ratio.
   */
  val minSample: Double
  abstract override def samplingFor(level: Int) =
    max(super.samplingFor(level), minSample)
}
