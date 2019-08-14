package com.velocidi.apso.elasticsearch

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches._
import io.circe.Json
import io.circe.syntax._
import net.ruippeixotog.akka.testkit.specs2.mutable.AkkaSpecification
import org.specs2.concurrent.ExecutionEnv

import com.velocidi.apso.elasticsearch.ElasticsearchBulkInserter.{ ElasticsearchDown, ElasticsearchUp, Insert }
import com.velocidi.apso.elasticsearch.config.Elasticsearch

class ElasticsearchBulkInserterSpec(implicit ee: ExecutionEnv) extends AkkaSpecification with ElasticsearchTestKit {

  def testBulkInserterConfig(maxBufferSize: Int, flushFreq: FiniteDuration) = {
    Elasticsearch.BulkInserter(
      flushFrequency = flushFreq,
      esDownCheckFrequency = 10.seconds,
      maxBufferSize = maxBufferSize,
      maxTryCount = 3)
  }

  class TestElasticsearchBulkInserter(
      maxBufferSize: Int = Int.MaxValue,
      flushFreq: FiniteDuration = 1.day,
      esStateListener: ActorRef = system.deadLetters)
    extends ElasticsearchBulkInserter(testBulkInserterConfig(maxBufferSize, flushFreq), esClient) {
    self ! esStateListener
  }

  private def searchQuery(msgIndex: String): SearchRequest =
    search(msgIndex)

  "An ElasticsearchBulkInserter" should {
    Seq("test-index-1", "test-index-2", "test-index-3").foreach(ensureIndexExists)

    "collect events and send them in bulk to Elasticsearch after a buffer is filled" in {
      val msgIndex = "test-index-1"
      val probe = TestProbe()

      val bulkInserter = system.actorOf(Props(new TestElasticsearchBulkInserter(
        maxBufferSize = 5, esStateListener = probe.ref)).withDispatcher("flush-prio-dispatcher"))

      probe must receiveWithin(10.seconds)(ElasticsearchUp)

      for (i <- 1 to 3) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must not(be_==(3).awaitFor(5.seconds).eventually(5, 2.seconds))

      for (i <- 4 to 5) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must be_==(5).awaitFor(5.seconds).eventually(30, 2.seconds)
    }

    "collect events and send them in bulk to Elasticsearch after a periodic flush occurs" in {
      val msgIndex = "test-index-2"
      val probe = TestProbe()

      val bulkInserter = system.actorOf(Props(new TestElasticsearchBulkInserter(
        flushFreq = 10.seconds, esStateListener = probe.ref)).withDispatcher("flush-prio-dispatcher"))

      probe must receiveWithin(10.seconds)(ElasticsearchUp)

      for (i <- 1 to 5) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)

      Thread.sleep(5000)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must be_==(0).awaitFor(5.seconds).eventually(10, 2.seconds)
      Thread.sleep(7500)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must be_==(5).awaitFor(5.seconds).eventually(10, 2.seconds)
    }

    "notify an actor when Elasticsearch changes its availability status" in {
      val msgIndex = "test-index-3"
      val probe = TestProbe()

      val bulkInserter = system.actorOf(Props(new TestElasticsearchBulkInserter(
        maxBufferSize = 5, esStateListener = probe.ref)).withDispatcher("flush-prio-dispatcher"))

      probe must receiveWithin(10.seconds)(ElasticsearchUp)

      for (i <- 1 to 3) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must be_==(0).awaitFor(5.seconds).eventually(10, 2.seconds)

      esClient.execute(closeIndex(msgIndex)).await
      probe must not(receiveMessage)

      for (i <- 4 to 5) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      probe must receive(ElasticsearchDown)

      esClient.execute(openIndex(msgIndex)).await
      probe must receiveWithin(15.seconds)(ElasticsearchUp)
      esClient.execute(searchQuery(msgIndex)).map(_.result.totalHits) must be_==(5).awaitFor(5.seconds).eventually(10, 2.seconds)
    }
  }

  override def afterAll() = {
    super.afterAll()
    shutdown()
  }
}
