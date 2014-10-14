package eu.shiftforward.apso.actor

import akka.testkit.TestKitBase
import org.specs2.execute.AsResult
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

trait ActorMatchers extends SpecificationLike {

  def receive(msg: Any): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsg(msg) must not(throwAn[Exception])
  }

  def receive(max: FiniteDuration, msg: Any): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsg(max, msg) must not(throwAn[Exception])
  }

  def receiveLike[R: AsResult](pf: PartialFunction[Any, R]): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgPF()(pf)
  }

  def receiveNoMessage: Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectNoMsg() must not(throwAn[Exception])
  }

  def receiveEventually(msg: Any): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.fishForMessage() {
      case `msg` => true
      case _ => false
    } must not(throwAn[Exception])
  }
}

