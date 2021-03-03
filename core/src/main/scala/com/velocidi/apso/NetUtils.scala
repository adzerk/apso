package com.velocidi.apso

import java.net.ServerSocket

/** Object containing network utility methods.
  */
object NetUtils {

  /** Returns an unused port.
    *
    * @return an unused port.
    */
  def availablePort(): Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
