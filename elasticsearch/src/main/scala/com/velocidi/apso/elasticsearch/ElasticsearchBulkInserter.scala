package com.velocidi.apso.elasticsearch

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

import akka.actor._
import akka.dispatch.ControlMessage
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.bulk.{ BulkResponse, BulkResponseItem }
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.{ ElasticClient, Indexable }
import io.circe.Json

import com.velocidi.apso.Logging

/**
 * An actor responsible for inserting tracking events into Elasticsearch.
 * This actor buffers requests until either the configured flush timer is
 * triggered or the buffer hits the max size.
 */
class ElasticsearchBulkInserter(esConfig: config.Elasticsearch)
  extends Actor with Logging {
  import ElasticsearchBulkInserter._

  implicit private[this] val ec: ExecutionContext = context.system.dispatcher

  private[this] val bulkInserterConfig = esConfig.bulkInserter.getOrElse {
    val fallback = config.Elasticsearch.BulkInserter(1.second, 10.seconds, 1000, 3)
    log.warn("Bulk inserter settings for sending documents to Elasticsearch were not found in the config. " +
      s"A default configuration will be used: $fallback")
    fallback
  }

  private[this] val maxBufferSize = bulkInserterConfig.maxBufferSize
  private[this] val maxTryCount = bulkInserterConfig.maxTryCount
  private[this] val flushFrequency = bulkInserterConfig.flushFrequency
  private[this] val esDownCheckFrequency = bulkInserterConfig.esDownCheckFrequency

  private[this] var client: ElasticClient = null
  private[this] var esStateListeners = List.empty[ActorRef]
  private[this] var buffer: List[Message] = Nil
  private[this] val tryCountMap = mutable.Map.empty[Message, Int]

  // override this when a failure to insert documents in ES is not considered critical
  protected[this] def logErrorsAsWarnings = false

  private[this] def logErrorOrWarning(msg: => String, throwable: Option[Throwable] = None): Unit = {
    (logErrorsAsWarnings, throwable) match {
      case (true, Some(t)) => log.warn(msg, t)
      case (true, None) => log.warn(msg)
      case (false, Some(t)) => log.error(msg, t)
      case (false, None) => log.error(msg)
    }
  }

  private[this] def checkElasticsearch(): Future[Boolean] = {
    client.execute(clusterHealth).map(_.result.status != "red")
  }

  private[this] def becomeElasticsearchUp(): Unit = {
    esStateListeners.foreach { _ ! ElasticsearchUp }
    val periodicFlush = context.system.scheduler.scheduleWithFixedDelay(flushFrequency, flushFrequency, self, Flush)
    context.become(elasticsearchUp(periodicFlush))
  }

  private[this] def becomeElasticsearchDown(): Unit = {
    esStateListeners.foreach { _ ! ElasticsearchDown }
    val periodicCheck = context.system.scheduler.scheduleWithFixedDelay(
      esDownCheckFrequency, esDownCheckFrequency, self, CheckElasticsearch)
    context.become(elasticsearchDown(periodicCheck))
  }

  private[this] def addRetry(msg: Message, item: BulkResponseItem) = {
    val tryCount = tryCountMap.getOrElse(msg, 0) + 1

    if (tryCount > maxTryCount) {
      tryCountMap.remove(msg)
      logErrorOrWarning(s"Error inserting document in Elasticsearch: $item")
      Nil
    } else {
      msg.sender ! Status.Failure(new Throwable(s"Error inserting document in Elasticsearch: ${item.error}"))
      tryCountMap(msg) = tryCount
      log.info(
        "Error inserting document in Elasticsearch: {}. Will retry {} more times",
        item.error, maxTryCount - tryCount)
      List(msg)
    }
  }

  private[this] def countSuccessfulMsgs(sentBuffer: Iterable[Message], bulkResponse: BulkResponse) = {
    val reqsAndRes = sentBuffer.iterator.zip(bulkResponse.items.iterator)

    val (successCount, failedReqs) = reqsAndRes.foldLeft((0, List.empty[Message])) {
      case ((count, failedReqs), (req, res)) if res.error.isDefined =>
        (count, failedReqs ::: addRetry(req, res))

      case ((count, failedReqs), (req, res)) => // if res.error.isEmpty...
        req.sender ! res.id
        tryCountMap.remove(req)
        (count + 1, failedReqs)
    }

    (successCount, failedReqs)
  }

  private[this] def flush() = {
    val sentBuffer = buffer
    buffer = Nil

    Try(client.execute(bulk(sentBuffer.map(_.msg): _*)).map(_.result)) match {
      case Success(bulkResponseFut: Future[BulkResponse]) =>
        bulkResponseFut.onComplete {
          case Success(bulkResponse) =>
            val (successCount, failedReqs) = countSuccessfulMsgs(sentBuffer, bulkResponse)

            if (successCount == 0) self ! ElasticsearchDown
            failedReqs.foreach(self ! _)

          case Failure(ex) =>
            // FIXME: there are some cases where a failure here indicates that Elasticsearch is down
            logErrorOrWarning("Error inserting documents in Elasticsearch.", Some(ex))
        }

      case Failure(_) =>
        self ! ElasticsearchDown
        sentBuffer.foreach(self ! _)
    }
  }

  override def preStart() = {
    client = ElasticsearchUtil.buildEsClient(esConfig)
    checkElasticsearch().onComplete {
      case Success(true) => self ! ElasticsearchUp
      case _ => self ! ElasticsearchDown
    }
  }

  def receive = init

  def init: Receive = {

    case ref: ActorRef =>
      esStateListeners = ref :: esStateListeners

    case insert: Insert => addMsgToBuffer(insert.toRequest)

    case request: IndexRequest => addMsgToBuffer(request)

    case msg: Message => buffer = msg :: buffer

    case ElasticsearchUp =>
      log.info("Elasticsearch is up. Bulk inserter will start sending requests")
      becomeElasticsearchUp()

    case ElasticsearchDown =>
      log.warn("Cannot connect to Elasticsearch. There may be some configuration problem or the cluster may be " +
        "temporarily down.")
      becomeElasticsearchDown()
  }

  def elasticsearchUp(periodicFlush: Cancellable): Receive = {

    case ref: ActorRef =>
      esStateListeners = ref :: esStateListeners
      ref ! ElasticsearchUp

    case insert: Insert =>
      addMsgToBuffer(insert.toRequest)
      if (buffer.lengthCompare(maxBufferSize) >= 0) flush()

    case request: IndexRequest =>
      addMsgToBuffer(request)
      if (buffer.lengthCompare(maxBufferSize) >= 0) flush()

    case msg: Message =>
      buffer = msg :: buffer
      if (buffer.lengthCompare(maxBufferSize) >= 0) flush()

    case Flush =>
      if (buffer.nonEmpty) flush()

    case ElasticsearchDown =>
      log.warn("Elasticsearch seems to be down. Waiting for cluster to recover")
      periodicFlush.cancel()
      becomeElasticsearchDown()
      context.system.scheduler.scheduleOnce(2.seconds, self, CheckElasticsearch)
  }

  def elasticsearchDown(periodicCheck: Cancellable): Receive = {

    case ref: ActorRef =>
      esStateListeners = ref :: esStateListeners
      ref ! ElasticsearchDown

    case insert: Insert => addMsgToBuffer(insert.toRequest)

    case request: IndexRequest => addMsgToBuffer(request)

    case msg: Message => buffer = msg :: buffer

    case CheckElasticsearch =>
      checkElasticsearch().collect { case true => self ! ElasticsearchUp }

    case ElasticsearchUp =>
      log.info("Elasticsearch is up. Bulk inserting started")
      periodicCheck.cancel()
      becomeElasticsearchUp()
      self ! Flush
  }

  override def postStop() = {
    if (buffer.nonEmpty) flush()
    client.close()
  }

  private def addMsgToBuffer(msg: IndexRequest) = buffer = Message(sender, msg) :: buffer
}

/**
 * Companion object for the `ElasticsearchBulkInserter` actor.
 */
object ElasticsearchBulkInserter extends Logging {
  implicit object JsonIndexable extends Indexable[Json] {
    def json(js: Json) = js.noSpaces
  }

  private case class Message(sender: ActorRef, msg: IndexRequest)

  /**
   * Message containing an object to insert
   * @param obj the JSON object to publish
   */
  case class Insert(obj: Json, index: String) {
    def toRequest: IndexRequest = indexInto(index).doc(obj)
  }

  /**
   * Message to signal the `ElasticsearchBulkInserter` actor that it should flush the buffered requests.
   */
  case object Flush extends ControlMessage

  /**
   * Message to notify the `ElasticsearchBulkInserter` actor that Elasticsearch was deemed down.
   */
  case object ElasticsearchDown extends ControlMessage

  /**
   * Message to notify the `ElasticsearchBulkInserter` actor that Elasticsearch was deemed up.
   */
  case object ElasticsearchUp extends ControlMessage

  /**
   * Message to signal the `ElasticsearchBulkInserter` actor that an Elasticsearch check is to be performed.
   */
  case object CheckElasticsearch extends ControlMessage

  def props(esConfig: config.Elasticsearch): Props = Props(new ElasticsearchBulkInserter(esConfig))
}
