package com.kevel.apso.caching

import scala.concurrent.Future

private[caching] trait SyncEvidence[A]

private[caching] object SyncEvidence extends LowPrioritySyncEvidence {
  implicit def ev1[A]: SyncEvidence[Future[A]] = new SyncEvidence[Future[A]] {}
  implicit def ev2[A]: SyncEvidence[Future[A]] = new SyncEvidence[Future[A]] {}
}

private[caching] trait LowPrioritySyncEvidence {
  implicit def ev[A]: SyncEvidence[A] = new SyncEvidence[A] {}
}
