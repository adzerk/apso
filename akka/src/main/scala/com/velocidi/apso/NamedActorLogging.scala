package com.velocidi.apso

import akka.actor.{Actor, ActorLogging}
import akka.event.Logging

trait NamedActorLogging extends ActorLogging { this: Actor =>
  override lazy val log = Logging(context.system.eventStream, self.path.toString)
}
