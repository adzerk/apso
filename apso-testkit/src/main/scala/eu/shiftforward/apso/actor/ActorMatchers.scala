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

  def receiveWhich[R: AsResult](f: Any => R): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgPF()(PartialFunction(f)) must not(throwAn[Exception])
  }

  def receiveWhich[R: AsResult](max: FiniteDuration)(f: Any => R): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgPF(max)(PartialFunction(f)) must not(throwAn[Exception])
  }

  def receiveLike[R: AsResult](pf: PartialFunction[Any, R]): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgPF()(pf)
  }

  def receiveLike[R: AsResult](max: FiniteDuration)(pf: PartialFunction[Any, R]): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgPF(max)(pf)
  }

  def receiveNoMessage: Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectNoMsg() must not(throwAn[Exception])
  }

  def receiveNoMessage(max: FiniteDuration): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectNoMsg(max) must not(throwAn[Exception])
  }

  def receiveEventually(msg: Any): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.fishForMessage()(PartialFunction(_ == msg)) must not(throwAn[Exception])
  }

  def receiveEventually(max: FiniteDuration, msg: Any): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.fishForMessage(max)(PartialFunction(_ == msg)) must not(throwAn[Exception])
  }

  def receiveAllOf(msg: Any*): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgAllOf(msg: _*) must not(throwAn[Exception])
  }

  def receiveAllOf(max: FiniteDuration, msg: Any*): Matcher[TestKitBase] = { probe: TestKitBase =>
    probe.expectMsgAllOf(max, msg: _*) must not(throwAn[Exception])
  }

  def receiveAllOfInOrder(msgList: Any*): Matcher[TestKitBase] = { probe: TestKitBase =>
    msgList.foreach { msg => probe.expectMsg(msg) } must not(throwAn[Exception])
  }

  def receiveAllOfInOrder(max: FiniteDuration, msgList: Any*): Matcher[TestKitBase] = { probe: TestKitBase =>
    msgList.foreach { msg => probe.expectMsg(max, msg) } must not(throwAn[Exception])
  }
}
