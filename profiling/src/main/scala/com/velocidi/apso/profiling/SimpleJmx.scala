package com.velocidi.apso.profiling

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import com.j256.simplejmx.server.JmxServer

import com.velocidi.apso.{Logging, NetUtils}

trait SimpleJmx extends Logging {

  def startJmx(jmxConfig: config.Jmx): Future[JmxServer] = {

    def tryStart(port: Option[Int] = None) = {
      val jmx = new JmxServer(port.getOrElse(NetUtils.availablePort()))
      jmx.start()
      jmx
    }

    // start a properly configured JMX server. When behind a firewall, both ports `jmxPort` (the RMI
    // registry port) and `jmxPort + 1` (the RMI server port) need to be open. Connections are
    // established through port `jmxPort`.
    // In the event of a binding failure to port `jmxPort`, a retry is performed with a random port.
    jmxConfig.host.foreach { host => System.setProperty("java.rmi.server.hostname", host) }

    Future {
      Try(tryStart(jmxConfig.port)).recover { case _ => tryStart() } match {
        case Success(jmx) =>
          log.info(s"Bound JMX on port ${jmx.getServerPort}")
          sys.addShutdownHook(jmx.stop())
          jmx
        case Failure(ex) =>
          log.warn("Could not start JMX server", ex)
          throw ex // produce a failed `Future`
      }
    }
  }
}
