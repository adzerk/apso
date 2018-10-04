package com.velocidi.apso

import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.scalacheck.Gen._

@deprecated("This will be removed in a future version", "2017/07/13")
class CounterPairSpec extends Specification with ScalaCheck {
  "A counter pair" should {
    "store two unsigned shorts" ! prop {
      (short1: Int, short2: Int) =>
        val CounterPair(x, y) = CounterPair(short1, short2)
        x == short1 && y == short2
    }.setGens(choose(0, 65535), choose(0, 65535))
  }
}
