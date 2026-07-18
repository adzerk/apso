package com.kevel.apso.caching

import scala.concurrent.Future
import scala.util.NotGiven

/** An evidence that an `A` is not a `Future[?]`. This class and respective companion object are public since they are
  * required by public methods, but they should not be used explicitly in user code.
  *
  * @tparam A
  *   the tagged type.
  */
class SyncEvidence[A]

object SyncEvidence {
  given [A](using NotGiven[A <:< Future[?]]): SyncEvidence[A] = SyncEvidence[A]
}
