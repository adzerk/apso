package com.velocidi.apso.pekko

import org.apache.pekko.actor.{Actor, ActorLogging}
import org.apache.pekko.event.Logging

trait NamedActorLogging extends ActorLogging { this: Actor =>
  override lazy val log = Logging(context.system.eventStream, self.path.toString)
}
