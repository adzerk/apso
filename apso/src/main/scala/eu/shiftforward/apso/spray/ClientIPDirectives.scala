package eu.shiftforward.apso.spray

import spray.http.HttpHeaders._
import spray.routing.Directive1
import spray.routing.directives.{ BasicDirectives, HeaderDirectives }

/**
 * Directives to extract the value of the Client IP.
 */
trait ClientIPDirectives extends BasicDirectives with HeaderDirectives {

  /**
   * Directive extracting the raw IP of the client from either the X-Forwarded-For, Remote-Address or X-Real-IP header
   * (in that order of priority).
   */
  def optionalRawClientIP: Directive1[Option[String]] =
    // This isn't implemented using spray's `clientIp` directive since it is not guaranteed that we
    // have a valid `RemoteAddress` as the header value (e.g. due to IP anonymization).
    headerValuePF { case h if h.is("x-forwarded-for") => Some(h.value) } |
      headerValuePF { case h if h.is("remote-address") => Some(h.value) } |
      headerValuePF { case h if h.is("x-real-ip") => Some(h.value) } |
      provide(None)
}
