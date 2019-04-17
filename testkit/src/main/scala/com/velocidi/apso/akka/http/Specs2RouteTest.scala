package com.velocidi.apso.akka.http

import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import org.specs2.execute.{ Failure, FailureException }

// until akka-http gets support
trait Specs2Interface extends TestFrameworkInterface {
  // def cleanUp(): Unit

  // from spray-testkit
  def failTest(msg: String): Nothing = {
    val trace = new Exception().getStackTrace.toList
    val fixedTrace = trace.drop(trace.indexWhere(_.getClassName.startsWith("org.specs2")) - 1)
    throw FailureException(Failure(msg, stackTrace = fixedTrace))
  }
}

trait Specs2RouteTest extends RouteTest with Specs2Interface
