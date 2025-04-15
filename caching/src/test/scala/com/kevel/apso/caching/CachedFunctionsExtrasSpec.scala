package com.kevel.apso.caching

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.{Level, Logger}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.AsResult
import org.specs2.mutable.Specification

class CachedFunctionsExtrasSpec(implicit ee: ExecutionEnv) extends Specification {
  "Cached extension methods in FunctionN types" should {

    "provide synchronous variants" in {
      "for multiple number of arguments" in {
        val f = () => 1
        val f2 = (_: Int) => 2
        val f3 = (_: Int, _: Int) => "hello"

        // format: off
        val f4: (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int) => String =
          (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) =>
            "dummy"
        // format: on

        f.cachedSync(config.Cache(None))() must beEqualTo(1)
        f2.cachedSync(config.Cache(None))(0) must beEqualTo(2)
        f3.cachedSync(config.Cache(None))(0, 1) must beEqualTo("hello")
        f4.cachedSync(config.Cache(None))(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
          22) must beEqualTo("dummy")
      }

      "respecting key-value semantics" in {
        val f: Int => Int = identity
        val cachedF = f.cachedSync(config.Cache(None))
        cachedF(1) must beEqualTo(1)
        cachedF(2) must beEqualTo(2)
        cachedF(2) must beEqualTo(2)
      }

      "respecting the time to live" in {
        val counter = new AtomicInteger(0)
        val f = () => counter.getAndIncrement()
        val cachedF = f.cachedSync(config.Cache(Some(1.second)))

        cachedF() must beEqualTo(0)
        eventually(retries = 2, sleep = 1.second)(cachedF() must beEqualTo(1))
      }

      "evicting quickly if the size is 0" in {
        def quickly[T: AsResult](result: => T): T = eventually(10, _ => 20.millis)(result)
        val counter = new AtomicInteger(0)
        val f = () => counter.getAndIncrement()
        val cachedF = f.cachedSync(config.Cache(Some(1.day), Some(0)))
        cachedF() must beEqualTo(0)
        quickly(cachedF() must beEqualTo(1))
        quickly(cachedF() must beEqualTo(2))
      }

      "that resorts to the `hashCode` method of the key" in {
        val hashCodeCallCounter = new AtomicInteger(0)
        val toStringCallCounter = new AtomicInteger(0)
        case class Dummy(str: String, int: Int) {
          override def hashCode(): Int = {
            hashCodeCallCounter.incrementAndGet()
            super.hashCode()
          }

          override def toString: String = {
            toStringCallCounter.incrementAndGet()
            super.toString
          }
        }

        val f = (dum: Dummy) => ()
        val cachedF = f.cachedSync(config.Cache(None))
        val dummy1 = Dummy("str", 0)
        hashCodeCallCounter.get() must beEqualTo(0)
        toStringCallCounter.get() must beEqualTo(0)

        // The first get will trigger a put, hence the two expected `hashCode` calls.
        // We want to make sure Caffeine does not resort to `toString` behind our backs.
        cachedF(dummy1) must beEqualTo(())
        hashCodeCallCounter.get() must beEqualTo(2)
        cachedF(dummy1) must beEqualTo(())
        hashCodeCallCounter.get() must beEqualTo(3)
        cachedF(dummy1) must beEqualTo(())
        hashCodeCallCounter.get() must beEqualTo(4)
        toStringCallCounter.get() must beEqualTo(0)

        // We tuple the arguments to build a key instance,
        // and need to ensure that `hashCode` is called recursively.
        val f2 = (dum1: Dummy, dum2: Dummy) => ()
        val cachedF2 = f2.cachedSync(config.Cache(None))
        val dummy2 = Dummy("str2", 0)
        val dummy3 = Dummy("str3", 0)
        hashCodeCallCounter.get() must beEqualTo(4)
        cachedF2(dummy2, dummy3) must beEqualTo(())
        hashCodeCallCounter.get() must beEqualTo(8)
        cachedF2(dummy2, dummy3) must beEqualTo(())
        hashCodeCallCounter.get() must beEqualTo(10)
        toStringCallCounter.get() must beEqualTo(0)
      }
    }

    "provide asynchronous variants" in {
      "for multiple number of arguments" in {
        val f = () => Future.successful(1)
        val f2 = (_: Int) => Future.successful(2)
        val f3 = (_: Int, _: Int) => Future.successful("hello")

        // format: off
        val f4: (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int) => Future[String] =
          (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) =>
            Future.successful("dummy")
        // format: on

        f.cachedAsync(config.Cache(None))() must beEqualTo(1).await
        f2.cachedAsync(config.Cache(None))(0) must beEqualTo(2).await
        f3.cachedAsync(config.Cache(None))(0, 1) must beEqualTo("hello").await
        f4.cachedAsync(config.Cache(None))(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
          22) must beEqualTo("dummy").await
      }

      "evicting failing futures" in {
        val counter = new AtomicInteger(0)
        val f = () => {
          val current = counter.getAndIncrement()
          if (current == 0) Future.failed(new RuntimeException()) else Future { current }
        }
        val cachedF = f.cachedAsync(config.Cache(Some(1.day)))

        // We are simulating a cache load failure here, so silence the expected exception log.
        val logger = Logger.getLogger("com.github.benmanes.caffeine.cache.LocalAsyncCache")
        logger.setLevel(Level.OFF)

        cachedF() must throwA[RuntimeException].await
        cachedF() must beEqualTo(1).await
        cachedF() must beEqualTo(1).await
      }
    }
  }
}
