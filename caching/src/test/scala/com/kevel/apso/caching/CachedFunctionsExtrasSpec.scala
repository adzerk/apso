package com.kevel.apso.caching

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class CachedFunctionsExtrasSpec(implicit ee: ExecutionEnv) extends Specification {
  "Cached extension methods in FunctionN types" should {

    "provide synchronous variants" in {
      "for multiple number of arguments" in {
        val f = () => 1
        val f2 = (_: Int) => 2
        val f3 = (_: Int, _: Int) => "hello"
        type D = Int
        val f4: (D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D) => String =
          (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) =>
            "dummy"
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
        Thread.sleep(500)
        cachedF() must beEqualTo(0)
        Thread.sleep(1000)
        cachedF() must beEqualTo(1)
      }

      "evicting right away if the size is 0" in {
        val counter = new AtomicInteger(0)
        val f = () => counter.getAndIncrement()
        val cachedF = f.cachedSync(config.Cache(Some(1.day), Some(0)))
        cachedF() must beEqualTo(0)
        eventually(cachedF() must beEqualTo(1))
        eventually(cachedF() must beEqualTo(2))
      }
    }

    "provide asynchronous variants" in {
      "for multiple number of arguments" in {
        val f = () => Future.successful(1)
        val f2 = (_: Int) => Future.successful(2)
        val f3 = (_: Int, _: Int) => Future.successful("hello")
        type D = Int
        val f4: (D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D, D) => Future[String] =
          (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22) =>
            Future.successful("dummy")
        f.cachedAsync(config.Cache(None))() must beEqualTo(1).await
        f2.cachedAsync(config.Cache(None))(0) must beEqualTo(2).await
        f3.cachedAsync(config.Cache(None))(0, 1) must beEqualTo("hello").await
        f4.cachedAsync(config.Cache(None))(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
          22) must beEqualTo("dummy").await
      }

      "evicting failing futures" in {
        val counter = new AtomicInteger(0)
        val f = () => if (counter.getAndIncrement() == 0) Future.failed(new RuntimeException()) else Future { counter }
        val cachedF = f.cachedAsync(config.Cache(Some(1.day)))
        Await.result(cachedF(), 10.seconds) must throwA
        cachedF() must beEqualTo(0)
        cachedF() must beEqualTo(0)
      }
    }
  }
}
