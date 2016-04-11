package eu.shiftforward.apso.spray

import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http
import spray.http.{ HttpHeader, HttpRequest, Uri }
import spray.routing.{ RequestContext, Route }

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
trait ProxySupport {

  private def sending(f: RequestContext => HttpRequest)(implicit system: ActorSystem): Route = {
    val transport = IO(Http)(system)
    ctx => transport.tell(f(ctx), ctx.responder)
  }

  private def stripHost(headers: List[HttpHeader] = Nil) = {
    headers.filterNot(header => header.is("host"))
  }

  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx => ctx.request.copy(
      uri = uri,
      headers = stripHost(ctx.request.headers)))
  }

  def proxyToUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx => ctx.request.copy(
      uri = uri.withPath(uri.path.++(ctx.unmatchedPath)).withQuery(ctx.request.uri.query),
      headers = stripHost(ctx.request.headers)))
  }
}
