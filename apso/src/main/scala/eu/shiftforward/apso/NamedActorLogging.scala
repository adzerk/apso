package eu.shiftforward.apso

import akka.actor.{ Actor, ActorLogging }
import akka.event.{ Logging => AkkaLogging }

trait NamedActorLogging extends ActorLogging { this: Actor =>
  override lazy val log = AkkaLogging(context.system.eventStream, self.path.toString)
}
