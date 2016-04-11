package eu.shiftforward.apso.spray

import spray.routing.Directive1
import spray.routing.directives.HeaderDirectives

/**
 * Directives to extract the value of the Client IP.
 */
trait ClientIPDirectives extends HeaderDirectives {

  /**
   * Directive extracting the raw IP of the client from either the X-Forwarded-For, Remote-Address or X-Real-IP header
   * (in that order of priority).
   */
  def optionalRawClientIP: Directive1[Option[String]] =
    optionalHeaderValuePF { case h if h.is("x-forwarded-for") => h.value } |
      optionalHeaderValuePF { case h if h.is("remote-address") => h.value } |
      optionalHeaderValuePF { case h if h.is("x-real-ip") => h.value }
}
