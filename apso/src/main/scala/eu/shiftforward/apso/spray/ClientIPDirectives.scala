package eu.shiftforward.apso.spray

import spray.http.RemoteAddress
import spray.routing.Directive1
import spray.routing.directives.{ BasicDirectives, HeaderDirectives, MiscDirectives }

/**
 * Directives to extract the value of the Client IP.
 */
trait ClientIPDirectives extends BasicDirectives with HeaderDirectives with MiscDirectives {

  /**
   * Directive extracting the RemoteAddress of the client from either the X-Forwarded-For, Remote-Address or X-Real-IP header
   * (in that order of priority).
   */
  def optionalClientIP: Directive1[Option[RemoteAddress]] =
    clientIP.map(Option(_)).recoverPF {
      case Nil => provide(None)
    }

  /**
   * Directive extracting the raw IP of the client from either the X-Forwarded-For, Remote-Address or X-Real-IP header
   * (in that order of priority).
   */
  def optionalRawClientIP: Directive1[Option[String]] =
    // This isn't implemented using spray's `clientIp` directive since it is not guaranteed that we
    // have a valid `RemoteAddress` as the header value (e.g. due to IP anonymization).
    headerValuePF { case h if h.is("x-forwarded-for") => h.value.split(",").headOption } |
      headerValuePF { case h if h.is("remote-address") => Option(h.value) } |
      headerValuePF { case h if h.is("x-real-ip") => Option(h.value) } |
      provide(None)
}
