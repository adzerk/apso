package com.velocidi.apso

import _root_.akka.actor.{ Actor, ActorLogging }
import _root_.akka.event.{ Logging => AkkaLogging }

trait NamedActorLogging extends ActorLogging { this: Actor =>
  override lazy val log = AkkaLogging(context.system.eventStream, self.path.toString)
}
