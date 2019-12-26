package com.velocidi.apso.akka.http

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._

/**
 * Directives to extract the value of the clients' IP addresses.
 */
trait ClientIPDirectives {

  /**
   * Directive extracting the remote address of the client from the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP`
   * headers (in that order).
   */
  def optionalClientIP: Directive1[Option[RemoteAddress]] =
    extractClientIP.map(Option(_)).recoverPF {
      case Nil => provide(None)
    }

  /**
   * Directive extracting the raw IP of the client from either the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP`
   * headers (in that order).
   */
  // This isn't implemented using spray's `clientIp` directive since it is not guaranteed that we
  // have a valid `RemoteAddress` as the header value (e.g. due to IP anonymization).
  def optionalRawClientIP: Directive1[Option[String]] =
    headerValuePF { case h if h.is("x-forwarded-for") => h.value.split(",").headOption } |
      headerValuePF { case h if h.is("remote-address") => Option(h.value) } |
      headerValuePF { case h if h.is("x-real-ip") => Option(h.value) } |
      provide(None)
}

object ClientIPDirectives extends ClientIPDirectives
