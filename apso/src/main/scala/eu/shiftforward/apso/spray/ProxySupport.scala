package eu.shiftforward.apso.spray

import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http
import spray.http.{ HttpHeader, HttpRequest, RemoteAddress, Uri }
import spray.http.HttpHeaders.{ `Remote-Address`, `X-Forwarded-For` }
import spray.routing.directives.HeaderDirectives._
import spray.routing.{ Directive1, RequestContext, Route }

/**
 * Allows to proxy a request to another URI.
 *
 * Defines two public Routes to proxy requests:
 * - `proxyTo` takes the original request and proxy's it to the proxy URI
 * - `proxyToUnmatchedPath` copies only the unmatched path from the original URI, and adds it to the path of the
 * proxy URI
 *
 *
 * Taken from: https://github.com/bthuillier/spray/pull/1
 *
 * @see https://github.com/akka/akka/issues/16844
 * @see https://github.com/spray/spray/pull/764/
 * @see https://github.com/bthuillier/spray/pull/1
 */
trait ProxySupport extends ClientIPDirectives {

  private def sending(f: RequestContext => HttpRequest)(implicit system: ActorSystem): Route = {
    val transport = IO(Http)(system)
    ctx => transport.tell(f(ctx), ctx.responder)
  }

  private def getHeaders(ip: Option[RemoteAddress], headers: List[HttpHeader] = Nil) = {
    // filter `Host` header
    val hs = headers.filterNot(header => header.is("host"))
    // add `X-Forwarded-For` header
    ip.fold(hs)(addForwardedFor(_, hs))
  }

  private def addForwardedFor(ip: RemoteAddress, headers: List[HttpHeader]): List[HttpHeader] = {
    headers match {
      case Nil =>
        // No `X-Forwarded-For` found in headers, so just add the new one
        `X-Forwarded-For`(ip) :: Nil

      case `X-Forwarded-For`(ips) :: tail =>
        `X-Forwarded-For`(ips :+ ip) :: tail

      case notForwardedFor :: tail =>
        notForwardedFor :: addForwardedFor(ip, tail)
    }
  }

  private val optionalRemoteAddress: Directive1[Option[RemoteAddress]] =
    headerValuePF { case `Remote-Address`(address) ⇒ Some(address) } | provide(None)

  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    optionalRemoteAddress { ip =>
      sending(ctx => ctx.request.copy(
        uri = uri,
        headers = getHeaders(ip, ctx.request.headers)))
    }
  }

  def proxyToUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = {
    optionalRemoteAddress { ip =>
      sending { ctx =>
        ctx.request.copy(
          uri = uri.withPath(uri.path.++(ctx.unmatchedPath)).withQuery(ctx.request.uri.query),
          headers = getHeaders(ip, ctx.request.headers))
      }
    }
  }
}
