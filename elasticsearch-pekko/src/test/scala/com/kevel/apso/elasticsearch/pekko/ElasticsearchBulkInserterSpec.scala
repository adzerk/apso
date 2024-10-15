package com.kevel.apso.elasticsearch.pekko

import scala.concurrent.duration._

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RequestSuccess
import com.sksamuel.elastic4s.requests.searches._
import io.circe.Json
import io.circe.syntax._
import org.apache.pekko.actor._
import org.apache.pekko.testkit._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationLike

import com.kevel.apso.elasticsearch.pekko.ElasticsearchBulkInserter.Insert
import com.kevel.apso.elasticsearch.pekko.config.Elasticsearch

class ElasticsearchBulkInserterSpec(implicit ee: ExecutionEnv)
    extends TestKit(ActorSystem("es-bulk-inserter-test"))
    with SpecificationLike
    with ElasticsearchTestKit {

  def testBulkInserterConfig(maxBufferSize: Int, flushFreq: FiniteDuration) = {
    Elasticsearch(
      host = "localhost",
      port = httpPort,
      useHttps = false,
      username = None,
      password = None,
      bulkInserter = Some(
        Elasticsearch.BulkInserter(
          flushFrequency = flushFreq,
          esDownCheckFrequency = 10.seconds,
          maxBufferSize = maxBufferSize,
          maxTryCount = 3
        )
      )
    )
  }

  def testBulkInserter(
      maxBufferSize: Int = Int.MaxValue,
      flushFreq: FiniteDuration = 1.day,
      esStateListener: ActorRef = system.deadLetters
  ): ActorRef = {
    val actorRef = system.actorOf(
      ElasticsearchBulkInserter
        .props(testBulkInserterConfig(maxBufferSize, flushFreq))
        .withDispatcher("flush-prio-dispatcher")
    )
    actorRef ! esStateListener
    actorRef
  }

  private def searchQuery(msgIndex: String): SearchRequest =
    search(msgIndex)

  private def numberOfHits(index: String) = {
    esClient.execute(searchQuery(index)).map(_.result.totalHits)
  }

  private implicit class ExtraMatchers[T](m: Matcher[T]) {
    def retry(
        awaitFor: FiniteDuration = 5.seconds,
        eventuallyRetries: Int = 5,
        eventuallyWait: FiniteDuration = 2.seconds
    ) = m.awaitFor(awaitFor).eventually(eventuallyRetries, eventuallyWait)
  }

  private val probeTimeout = 1.second

  "An ElasticsearchBulkInserter" should {
    Seq("test-index-1", "test-index-2", "test-index-3", "test-index-4", "test-index-5", "test-index-6").foreach(
      ensureIndexExists
    )

    "collect events and send them in bulk to Elasticsearch after a buffer is filled" in {
      val msgIndex = "test-index-1"

      val bulkInserter = testBulkInserter(maxBufferSize = 5)

      for (i <- 1 to 3) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      numberOfHits(msgIndex) must not(be_==(3L).retry())

      for (i <- 4 to 5) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)
      numberOfHits(msgIndex) must be_==(5L).retry(eventuallyRetries = 30)
    }

    "collect events and send them in bulk to Elasticsearch after a periodic flush occurs" in {
      val msgIndex = "test-index-2"

      val bulkInserter = testBulkInserter(flushFreq = 10.seconds)

      for (i <- 1 to 5) bulkInserter ! Insert(Json.obj("id" := i), msgIndex)

      Thread.sleep(5000)
      numberOfHits(msgIndex) must be_==(0L).retry(eventuallyRetries = 10)
      Thread.sleep(7500)
      numberOfHits(msgIndex) must be_==(5L).retry(eventuallyRetries = 10)
    }

    "correctly handle errors and retry document insertion errors" in {
      val msgIndex = "test-index-4"

      val bulkInserter = testBulkInserter(maxBufferSize = 2)

      // use a mapping that does not allow for extra fields other than the "name" one
      esClient.execute(
        putMapping(msgIndex) rawSource """{"dynamic":"strict","properties":{"name":{"type":"text"}}}"""
      ) must
        beAnInstanceOf[RequestSuccess[_]].awaitFor(5.seconds)

      // insert a (valid) document
      bulkInserter ! Insert(Json.obj("name" := "test1"), msgIndex)
      bulkInserter ! Insert(Json.obj("name" := "test2"), msgIndex)
      numberOfHits(msgIndex) must be_==(2L).retry(eventuallyRetries = 30)

      // try to insert a (invalid) document; this one should stay on the buffer for retry later on...
      bulkInserter ! Insert(Json.obj("name" := "test3"), msgIndex)
      bulkInserter ! Insert(Json.obj("name" := "test4", "other" := "dynamic_field_value"), msgIndex)
      numberOfHits(msgIndex) must not(be_==(4L).retry())
      numberOfHits(msgIndex) must be_==(3L).retry()

      // now, change the mapping so that new fields are allowed...
      esClient.execute(putMapping(msgIndex) rawSource """{"dynamic":true,"properties":{"name":{"type":"text"}}}""") must
        beAnInstanceOf[RequestSuccess[_]].awaitFor(5.seconds)
      bulkInserter ! Insert(Json.obj("name" := "test5"), msgIndex)
      numberOfHits(msgIndex) must be_==(5L).retry(eventuallyRetries = 30)
      esClient.execute(search(msgIndex) query matchQuery("other", "dynamic_field_value")).map(_.result.totalHits) must
        be_==(1L).retry(eventuallyRetries = 30)
    }

    "correctly send error back to client only when retries are exhausted" in {
      "when the buffer maximum size is reached" in {
        val msgIndex = "test-index-5"
        // maxTryCount is 3
        val bulkInserter = testBulkInserter(maxBufferSize = 2, flushFreq = 1.day)

        // use a mapping that does not allow for extra fields other than the "name" one
        esClient.execute(
          putMapping(msgIndex) rawSource """{"dynamic":"strict","properties":{"name":{"type":"text"}}}"""
        ) must
          beAnInstanceOf[RequestSuccess[_]].awaitFor(5.seconds)

        // insert valid documents
        bulkInserter ! Insert(Json.obj("name" := "test1"), msgIndex)
        bulkInserter ! Insert(Json.obj("name" := "test2"), msgIndex)
        numberOfHits(msgIndex) must be_==(2L).retry(eventuallyRetries = 30)

        // insert an invalid document that will be retried
        val probe = TestProbe()
        bulkInserter.!(Insert(Json.obj("name" := "test3", "other" := "dynamic_field_value"), msgIndex))(probe.ref)
        numberOfHits(msgIndex) must be_==(2L).retry(eventuallyRetries = 30)
        probe.receiveOne(probeTimeout) must not(beLike { case Status.Failure(_) => ok })

        // after this valid one, the failed one above will be retried...
        bulkInserter ! Insert(Json.obj("name" := "test4"), msgIndex)
        numberOfHits(msgIndex) must be_==(3L).retry(eventuallyRetries = 30)
        probe.receiveOne(probeTimeout) must not(beLike { case Status.Failure(_) => ok })

        // after this valid one, the failed one above will be retried...
        bulkInserter ! Insert(Json.obj("name" := "test5"), msgIndex)
        numberOfHits(msgIndex) must be_==(4L).retry(eventuallyRetries = 30)
        probe.receiveOne(probeTimeout) must not(beLike { case Status.Failure(_) => ok })

        // after this valid one, the failed one above will be retried...
        bulkInserter ! Insert(Json.obj("name" := "test6"), msgIndex)
        numberOfHits(msgIndex) must be_==(5L).retry(eventuallyRetries = 30)
        probe.receiveOne(probeTimeout) must not(beLike { case Status.Failure(_) => ok })

        // after this valid one, finally the error will be sent back to the client
        bulkInserter ! Insert(Json.obj("name" := "test7"), msgIndex)
        numberOfHits(msgIndex) must be_==(6L).retry(eventuallyRetries = 30)
        probe.receiveOne(probeTimeout) must beLike { case Status.Failure(_) => ok }
      }

      "when the buffer is automatically flushed on schedule" in {
        val msgIndex = "test-index-6"
        // maxTryCount is 3
        val bulkInserter = testBulkInserter(maxBufferSize = 2, flushFreq = 500.millis)

        // use a mapping that does not allow for extra fields other than the "name" one
        esClient.execute(
          putMapping(msgIndex) rawSource """{"dynamic":"strict","properties":{"name":{"type":"text"}}}"""
        ) must
          beAnInstanceOf[RequestSuccess[_]].awaitFor(5.seconds)

        // insert an invalid document that will be retried
        val probe = TestProbe()
        bulkInserter.!(Insert(Json.obj("name" := "test3", "other" := "dynamic_field_value"), msgIndex))(probe.ref)
        numberOfHits(msgIndex) must be_==(0L).retry(eventuallyRetries = 30)
        eventually(30, 2.seconds) {
          probe.receiveOne(probeTimeout) must beLike { case Status.Failure(_) => ok }
        }
      }
    }
  }

  override def afterAll() = {
    super.afterAll()
    shutdown()
  }
}
