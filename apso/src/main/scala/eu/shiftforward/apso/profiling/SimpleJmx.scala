package eu.shiftforward.apso.profiling

import com.j256.simplejmx.server.JmxServer
import com.typesafe.config.Config
import eu.shiftforward.apso.Logging

trait SimpleJmx extends Logging {
  def jmxConfig: Config

  lazy val jmxHost = jmxConfig.getString("host")
  lazy val jmxPort = jmxConfig.getInt("port")

  System.setProperty("java.rmi.server.hostname", jmxHost)

  // start a properly configured JMX server. When behind a firewall, both ports `jmxPort` (the RMI
  // registry port) and `jmxPort + 1` (the RMI server port) need to be open. Connections are established
  // through port `jmxPort`.
  try {
    val jmx = new JmxServer(jmxPort)
    jmx.start()
    log.info("Bound JMX on port {}", jmxPort)
    sys.addShutdownHook(jmx.stop())
    jmx
  } catch {
    case e: Exception => log.warn("Could not start JMX server", e)
  }
}
