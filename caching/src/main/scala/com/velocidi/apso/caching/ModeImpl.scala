package com.velocidi.apso.caching

import scala.concurrent.{ExecutionContext, Future}

import scalacache.{Id, Mode}

/** Represents a container mode for a cached value.
  * @tparam F
  *   the container type, e.g., Future, Try.
  */
trait ModeImpl[F[_]] {
  def mode: Mode[F]
}

object ModeImpl {
  implicit def futureModeImpl(implicit ec: ExecutionContext): ModeImpl[Future] = new ModeImpl[Future] {
    lazy val mode: Mode[Future] = scalacache.modes.scalaFuture.mode
  }

  /** A mode for values that have no container.
    */
  implicit val idModeImpl: ModeImpl[Id] = new ModeImpl[Id] {
    lazy val mode: Mode[Id] = scalacache.modes.sync.mode
  }
}
