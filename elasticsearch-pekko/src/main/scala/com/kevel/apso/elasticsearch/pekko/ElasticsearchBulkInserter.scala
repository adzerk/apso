package com.kevel.apso.elasticsearch.pekko

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.bulk.{BulkResponse, BulkResponseItem}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.{ElasticClient, Indexable}
import io.circe.Json
import org.apache.pekko.actor._
import org.apache.pekko.dispatch.ControlMessage

/** An actor responsible for inserting tracking events into Elasticsearch. This actor buffers requests until either the
  * configured flush timer is triggered or the buffer hits the max size.
  */
class ElasticsearchBulkInserter(
    esConfig: config.Elasticsearch,
    logErrorsAsWarnings: Boolean,
    timeoutOnStop: FiniteDuration = 3.seconds
) extends Actor
    with ActorLogging {
  import ElasticsearchBulkInserter._

  implicit private[this] val ec: ExecutionContext = context.system.dispatcher

  private[this] val bulkInserterConfig = esConfig.bulkInserter.getOrElse {
    val fallback = config.Elasticsearch.BulkInserter(1.second, 10.seconds, 1000, 3)
    logErrorOrWarning(
      "Bulk inserter settings for sending documents to Elasticsearch were not found in the config. " +
        s"A default configuration will be used: $fallback"
    )
    fallback
  }

  private[this] val maxBufferSize = bulkInserterConfig.maxBufferSize
  private[this] val maxTryCount = bulkInserterConfig.maxTryCount
  private[this] val flushFrequency = bulkInserterConfig.flushFrequency
  private[this] val esDownCheckFrequency = bulkInserterConfig.esDownCheckFrequency

  private[this] var client: ElasticClient = null
  private[this] var buffer: List[Message] = Nil
  private[this] val tryCountMap = mutable.Map.empty[Message, Int]

  private[this] def logErrorOrWarning(msg: => String, throwable: Option[Throwable] = None): Unit = {
    (logErrorsAsWarnings, throwable) match {
      case (true, Some(t))  => log.warning(s"$msg\n$t")
      case (true, None)     => log.warning(msg)
      case (false, Some(t)) => log.error(s"$msg\n$t")
      case (false, None)    => log.error(msg)
    }
  }

  private[this] def checkElasticsearch(): Future[Boolean] = {
    client.execute(clusterHealth()).map(_.result.status != "red")
  }

  private[this] def becomeElasticsearchUp(): Unit = {
    val periodicFlush = context.system.scheduler.scheduleWithFixedDelay(flushFrequency, flushFrequency, self, Flush)
    context.become(elasticsearchUp(periodicFlush))
  }

  private[this] def becomeElasticsearchDown(): Unit = {
    val periodicCheck = context.system.scheduler.scheduleWithFixedDelay(
      esDownCheckFrequency,
      esDownCheckFrequency,
      self,
      CheckElasticsearch
    )
    context.become(elasticsearchDown(periodicCheck))
  }

  private[this] def addRetry(msg: Message, item: BulkResponseItem) = {
    val tryCount = tryCountMap.getOrElse(msg, 0) + 1

    if (tryCount > maxTryCount) {
      msg.sender ! Status.Failure(new Throwable(s"Error inserting document in Elasticsearch: ${item.error}"))
      tryCountMap.remove(msg)
      logErrorOrWarning(s"Error inserting document in Elasticsearch: $item")
      Nil
    } else {
      tryCountMap(msg) = tryCount
      log.info(
        "Error inserting document in Elasticsearch: {}. Will retry {} more times",
        item.error,
        maxTryCount - tryCount
      )
      List(msg)
    }
  }

  /** Given a list of messages that were sent for indexing and the corresponding bulk response, it returns a list of
    * failed messages and notifies the sender of the successful ones. The retry counter of the failed messages is also
    * incremented.
    *
    * @param sentBuffer
    *   the list of messages that were sent for bulk indexing on Elasticsearch
    * @param bulkResponse
    *   the Elasticsearch response for the bulk indexing of the `sentBuffer`
    * @return
    *   an iterator of [[Message]] corresponding to those in `sentBuffer` that failed to index in Elasticsearch
    */
  private[this] def notifyOfSuccessfulAndGetFailed(
      sentBuffer: Iterable[Message],
      bulkResponse: BulkResponse
  ): Iterator[Message] = {
    val reqsAndRes = sentBuffer.iterator.zip(bulkResponse.items.iterator)

    reqsAndRes.flatMap { case (req, res) =>
      if (res.error.isDefined) {
        addRetry(req, res)
      } else {
        req.sender ! res.id
        tryCountMap.remove(req)
        Nil
      }
    }
  }

  private[this] def flush(): Future[BulkResponse] = {
    val sentBuffer = buffer
    buffer = Nil

    Try(client.execute(bulk(sentBuffer.map(_.msg): _*)).map(_.result)) match {
      case Success(bulkResponseFut: Future[BulkResponse]) =>
        bulkResponseFut.onComplete {
          case Success(bulkResponse) =>
            notifyOfSuccessfulAndGetFailed(sentBuffer, bulkResponse).foreach(self ! _)

          case Failure(_) =>
            self ! ElasticsearchDown
            sentBuffer.foreach(self ! _)
        }
        bulkResponseFut

      case Failure(ex) =>
        self ! ElasticsearchDown
        sentBuffer.foreach(self ! _)
        Future.failed(ex)
    }
  }

  override def preStart() = {
    client = ElasticsearchUtil.buildEsClient(esConfig)
    checkElasticsearch().onComplete {
      case Success(true) => self ! ElasticsearchUp
      case _             => self ! ElasticsearchDown
    }
  }

  def receive = init

  def init: Receive = {

    case insert: Insert => addMsgToBuffer(insert.toRequest)

    case request: IndexRequest => addMsgToBuffer(request)

    case msg: Message => buffer = msg :: buffer

    case ElasticsearchUp =>
      log.info("Elasticsearch is up. Bulk inserter will start sending requests")
      becomeElasticsearchUp()

    case ElasticsearchDown =>
      logErrorOrWarning(
        "Cannot connect to Elasticsearch. There may be some configuration problem or the cluster may be " +
          "temporarily down."
      )
      becomeElasticsearchDown()
  }

  def elasticsearchUp(periodicFlush: Cancellable): Receive = {

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
      logErrorOrWarning("Elasticsearch seems to be down. Waiting for cluster to recover")
      periodicFlush.cancel()
      becomeElasticsearchDown()
      context.system.scheduler.scheduleOnce(2.seconds, self, CheckElasticsearch)
  }

  def elasticsearchDown(periodicCheck: Cancellable): Receive = {

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
    super.postStop()

    log.info("Stopping Bulk Inserter...")
    val stop = if (buffer.nonEmpty) flush().andThen { case _ => client.close() }
    else Future(client.close())

    Try(Await.result(stop, timeoutOnStop)).failed.foreach { ex =>
      logErrorOrWarning("Failed to cleanly stop Bulk Inserter!", Some(ex))
    }
  }

  private def addMsgToBuffer(msg: IndexRequest) = buffer = Message(sender(), msg) :: buffer
}

/** Companion object for the `ElasticsearchBulkInserter` actor.
  */
object ElasticsearchBulkInserter {
  implicit object JsonIndexable extends Indexable[Json] {
    def json(js: Json) = js.noSpaces
  }

  private case class Message(sender: ActorRef, msg: IndexRequest)

  /** Message containing an object to insert
    * @param obj
    *   the JSON object to publish
    */
  case class Insert(obj: Json, index: String) {
    def toRequest: IndexRequest = indexInto(index).doc(obj)
  }

  /** Message to signal the `ElasticsearchBulkInserter` actor that it should flush the buffered requests.
    */
  case object Flush extends ControlMessage

  /** Message to notify the `ElasticsearchBulkInserter` actor that Elasticsearch was deemed down.
    */
  case object ElasticsearchDown extends ControlMessage

  /** Message to notify the `ElasticsearchBulkInserter` actor that Elasticsearch was deemed up.
    */
  case object ElasticsearchUp extends ControlMessage

  /** Message to signal the `ElasticsearchBulkInserter` actor that an Elasticsearch check is to be performed.
    */
  case object CheckElasticsearch extends ControlMessage

  /** Creates a Props for `ElasticsearchBulkInserter`.
    *
    * @param esConfig
    *   the elasticsearch configuration to use
    * @param logErrorsAsWarnings
    *   whether errors should be logged as warnings
    * @return
    *   a `Props` for `ElasticsearchBulkInserter`.
    */
  def props(esConfig: config.Elasticsearch, logErrorsAsWarnings: Boolean = false): Props =
    Props(new ElasticsearchBulkInserter(esConfig, logErrorsAsWarnings))
}
