package eu.shiftforward.apso.profiling

import java.net.ServerSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

import com.j256.simplejmx.server.JmxServer

import eu.shiftforward.apso.Logging

trait SimpleJmx extends Logging {
  def jmxConfig: config.Jmx

  private def startJmx(port: Option[Int] = None) = {
    def randomPort = {
      val s = new ServerSocket(0)
      val p = s.getLocalPort
      s.close()
      p
    }

    val jmx = new JmxServer(port.getOrElse(randomPort))
    jmx.start()
    jmx
  }

  // do all this asynchronously to avoid evaluating `jmxConfig` as soon as the object is initialized
  val jmxServer: Future[JmxServer] = Future {

    // start a properly configured JMX server. When behind a firewall, both ports `jmxPort` (the RMI
    // registry port) and `jmxPort + 1` (the RMI server port) need to be open. Connections are
    // established through port `jmxPort`.
    // In the event of a binding failure to port `jmxPort`, a retry is performed with a random port.
    jmxConfig.host.foreach { host => System.setProperty("java.rmi.server.hostname", host) }
    val jmxServer = Try(startJmx(jmxConfig.port)).recover { case _ => startJmx() }

    jmxServer match {
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
