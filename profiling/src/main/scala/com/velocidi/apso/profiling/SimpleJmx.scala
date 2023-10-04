package com.velocidi.apso.profiling

import java.net.ServerSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import com.j256.simplejmx.server.JmxServer
import com.typesafe.scalalogging.LazyLogging

trait SimpleJmx extends LazyLogging {

  def startJmx(jmxConfig: config.Jmx): Future[JmxServer] = {

    def tryStart(port: Option[Int] = None) = {
      // Returns an unused port.
      def availablePort(): Int = {
        val socket = new ServerSocket(0)
        val port = socket.getLocalPort
        socket.close()
        port
      }

      val jmx = new JmxServer(port.getOrElse(availablePort()))
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
          logger.info(s"Bound JMX on port ${jmx.getServerPort}")
          sys.addShutdownHook(jmx.stop())
          jmx
        case Failure(ex) =>
          logger.warn("Could not start JMX server", ex)
          throw ex // produce a failed `Future`
      }
    }
  }
}
