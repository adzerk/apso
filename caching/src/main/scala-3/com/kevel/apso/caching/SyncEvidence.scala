package com.kevel.apso.caching

import scala.concurrent.Future
import scala.util.NotGiven

/** An evidence that an `A` is not a `Future[_]`.
  *
  * @tparam A
  *   the tagged type.
  */
private[caching] trait SyncEvidence[A]

private[caching] object SyncEvidence {
  implicit def ev[A](implicit notFuture: NotGiven[A <:< Future[_]]): SyncEvidence[A] = new SyncEvidence[A] {}
}
