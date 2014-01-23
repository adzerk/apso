package eu.shiftforward.apso.actor

import akka.actor._
import akka.actor.Actor._
import scala.concurrent.duration._
import scala.reflect.ClassTag
import Retrier._

/**
 * Companion object for `Retrier`, containing extension methods for actor `Receive` actions.
 */
object Retrier {
  private case class Retry(to: ActorRef, msg: Any)
  private case class Timeout(groupId: Long)

  /**
   * Implicit class providing extension methods for actor `Receive` actions.
   * @param rec the `Receive` action
   */
  implicit class RetrierReceive(val rec: Receive) extends AnyVal {
    def orRetryWith[Req, Msg, Ack, Key](retrier: Retrier[Req, Msg, Ack, Key]): Receive =
      retrier.withRetry(rec)
  }
}

/**
 * Helper class for actors that need to retry some of the messages they send to other actors until
 * a certain acknowledgement message (ACK) is received. Messages can be sent individually or in
 * batches.
 *
 * This class is instantiated by providing functions that extract an identifier from sent messages
 * and from ACK messages. This can be an arbitrary identifier, as long as it uniquely associates a
 * received ACK with the original sent message. Optional per-message filtering functions can be
 * given, as well as the frequency of the retries and an optional timeout. Finally, the `onComplete`
 * method, which is executed after a message or group of messages is acknowledged, must be
 * implemented.
 *
 * A `Retrier` can be used as follows:
 *
 * {{{
 *   case class ChangeData(reqId: Long, data: String)
 *   case class ChangeDataAck(reqId: Long)
 *   case class Replicate(reqId: Long, data: String)
 *   case class ReplicateAck(reqId: Long)
 *
 *   class Master(val replica: ActorRef) extends Actor {
 *     import Retrier._
 *
 *     val retrier = new Retrier[(ActorRef, ChangeData), Replicate, ReplicateAck, Long](_.reqId, _.reqId) {
 *       def onComplete(req: (ActorRef, ChangeData)) = req._1 ! ChangeDataAck(req._2.reqId)
 *     }
 *
 *     def receive: Receive = ({
 *       case msg @ ChangeData(reqId, data) =>
 *         // change data internally here
 *         retrier.dispatch((sender, msg), Replicate(reqId, data), replica)
 *
 *     }: Receive).orRetryWith(retrier)
 *   }
 * }}}
 *
 * In the previous example, every time a `Master` actor receives a `ChangeData` message, it
 * sends a `Replicate` message to a replica actor and only responds to the original sender after an
 * acknowledgement from the replica is received. The `Replicate` message is retried periodically.
 *
 * @param msgKeyFunc a function that extracts a key out of a sent message
 * @param ackKeyFunc a function that extracts a key out of an acknowledgement message
 * @param isAck a function that defines which messages of type `Ack` should be considered
 *              acknowledgments
 * @param shouldRetry a function that defines if a message should be retried or not
 * @param retryDelay the retry delay
 * @param timeout an optional timeout
 * @param context the actor context
 * @param self the actor for which this `Retrier` was created
 * @tparam Req the type of the triggering request
 * @tparam Msg the type of the messages to be sent and retried
 * @tparam Ack the type of the ACK messages
 * @tparam Key the type of identifier or object that links a sent message to its ACK
 */
abstract class Retrier[Req, Msg: ClassTag, Ack: ClassTag, Key](
    msgKeyFunc: Msg => Key,
    ackKeyFunc: Ack => Key,
    isAck: Ack => Boolean = { _: Ack => true },
    shouldRetry: Msg => Boolean = { _: Msg => true },
    retryDelay: FiniteDuration = 100.millis,
    timeout: Option[FiniteDuration] = None)(
        implicit context: ActorContext, self: ActorRef) {

  implicit val dispatcher = context.dispatcher

  // generator for message group ids
  private[this] var _grpCounter = 0L
  private[this] def nextGroupId = {
    val ret = _grpCounter
    _grpCounter += 1
    ret
  }

  // map from message ids to message group id
  private[this] var acks = Map.empty[Key, Long]

  // map from message group ids to pairs (requester, request)
  private[this] var groupTrigger = Map.empty[Long, Req]

  // map from message group ids to pairs (message, destination)
  private[this] var ackGroups = Map.empty[Long, Set[(Msg, ActorRef)]]

  /**
   * Dispatch a single message. The `onComplete` method will be called once all the message is
   * acknowledged. If a timeout occurs, `onFailure` method is fired.
   * @param request the request that triggered this dispatch. Used only for identifying this batch
   *                when the `onComplete` and `onFailure` methods are called.
   * @param msg the message to send
   * @param to the destination of the message
   */
  def dispatch(request: Req, msg: Msg, to: ActorRef): Unit = dispatch(request, Set(msg -> to))

  /**
   * Dispatch a set of messages to their destinations as a batch. The `onComplete` method will only
   * be called once all sent messages are acknowledged. Retries are executed per message; a timeout
   * ceases all retries in the batch and causes the `onFailure` method to fire.
   * @param request the request that triggered this dispatch. Used only for identifying this batch
   *                when the `onComplete` and `onFailure` methods are called.
   * @param msgs the set of messages to sent and respective destinations
   */
  def dispatch(request: Req, msgs: Set[(Msg, ActorRef)]) {
    val groupId = nextGroupId
    msgs.foreach {
      case (msg, to) =>
        to ! msg
        acks += (msgKeyFunc(msg) -> groupId)
        if (shouldRetry(msg))
          context.system.scheduler.scheduleOnce(retryDelay, self, Retry(to, msg))
    }
    ackGroups += (groupId -> msgs)
    groupTrigger += (groupId -> request)
    timeout.foreach { context.system.scheduler.scheduleOnce(_, self, Timeout(groupId)) }
  }

  /**
   * Acknowledges immediately all messages sent to an actor.
   * @param to the destination of the messages to be acknowledged
   */
  def ackAll(to: ActorRef) {
    val ackedMsgs = ackGroups.values.flatten.filter(_._2 == to).map(_._1)
    for (msg <- ackedMsgs) ack(msgKeyFunc(msg))
  }

  private[this] def ack(ackId: Key) {
    val groupId = acks(ackId)
    acks -= ackId

    val newGroupMsgs = ackGroups(groupId).filterNot { key =>
      msgKeyFunc(key._1) == ackId
    }

    if (newGroupMsgs.isEmpty) {
      onComplete(groupTrigger(groupId))

      ackGroups -= groupId
      groupTrigger -= groupId

    } else {
      ackGroups += (groupId -> newGroupMsgs)
    }
  }

  /**
   * Augments a `Receive` action with retry-specific message handling.
   * @param rec the `Receive` action to augment
   * @return a new `Receive` capable of handling retry-specific messages.
   */
  def withRetry(rec: Receive): Receive = rec.orElse {

    case Retry(to, msg: Msg) if acks.contains(msgKeyFunc(msg)) =>
      to ! msg
      context.system.scheduler.scheduleOnce(retryDelay, self, Retry(to, msg))

    case Timeout(groupId) if ackGroups.contains(groupId) =>
      ackGroups(groupId).foreach {
        case (msg, to) =>
          acks -= msgKeyFunc(msg)
      }
      onFailure(groupTrigger(groupId))

      ackGroups -= groupId
      groupTrigger -= groupId

    case msg: Ack if isAck(msg) && acks.contains(ackKeyFunc(msg)) =>
      ack(ackKeyFunc(msg))
  }

  /**
   * Hook executed when a message or group of messages is acknowledged.
   * @param request the triggering request
   */
  def onComplete(request: Req)

  /**
   * Hook executed when a timeout occurs in a message or group of messages.
   * @param request the triggering request
   */
  def onFailure(request: Req) {}
}
