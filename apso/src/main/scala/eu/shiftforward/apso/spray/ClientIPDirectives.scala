package eu.shiftforward.apso.spray

import spray.http.RemoteAddress
import spray.routing.Directive1
import spray.routing.directives.{ BasicDirectives, MiscDirectives }

/**
 * Directives to extract the value of the Client IP.
 */
trait ClientIPDirectives extends BasicDirectives with MiscDirectives {

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
    optionalClientIP.map(_.map(_.value))
}
