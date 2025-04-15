package com.kevel.apso.caching

import scala.concurrent.Future

/** An evidence that an `A` is not a `Future[_]`. For this we use a trick similar to Shapeless `=!=` operator, i.e. we
  * induce an ambiguous implicit resolution for any `Future[_]`, with higher priority than the properly defined single
  * instance for any `A`. These implicits are available wherever the type is available.
  *
  * @tparam A
  *   the tagged type.
  */
private[caching] trait SyncEvidence[A]

private[caching] object SyncEvidence extends LowPrioritySyncEvidence {
  implicit def ev1[A]: SyncEvidence[Future[A]] = new SyncEvidence[Future[A]] {}
  implicit def ev2[A]: SyncEvidence[Future[A]] = new SyncEvidence[Future[A]] {}
}

private[caching] trait LowPrioritySyncEvidence {
  implicit def ev[A]: SyncEvidence[A] = new SyncEvidence[A] {}
}
