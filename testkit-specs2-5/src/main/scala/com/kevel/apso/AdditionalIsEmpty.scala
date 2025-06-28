package com.kevel.apso

import org.specs2.collection.IsEmpty

trait AdditionalIsEmpty {
  given mapIsEmpty[K, V]: IsEmpty[Map[K, V]] with {
    def isEmpty(t: Map[K, V]): Boolean =
      t.isEmpty
  }

  given mutMapIsEmpty[K, V]: IsEmpty[scala.collection.mutable.Map[K, V]] with {
    def isEmpty(t: scala.collection.mutable.Map[K, V]): Boolean =
      t.isEmpty
  }

  given setIsEmpty[T]: IsEmpty[Set[T]] with {
    def isEmpty(t: Set[T]): Boolean =
      t.isEmpty
  }
}

object AdditionalIsEmpty extends AdditionalIsEmpty
