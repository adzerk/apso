package com.kevel.apso

import org.specs2.matcher.*
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.dsl.*

/** Disables some `specs2` syntax, namely comparisons with `===` and `should` and expectation description methods that
  * tend to be ambiguous with other libraries. One is encouraged to write consistent expectations with the `must`
  * method.
  */
trait StrictMatching
    extends NoTypedEqual
    with NoShouldExpectations
    with NoExpectationsDescription
    with NoValueDescription
    with NoBangExamples
    with NoTitleDsl { self: SpecificationLike => }
