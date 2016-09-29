package eu.shiftforward.apso.akka.http

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ `Remote-Address`, `X-Forwarded-For` }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{ Directive1, Route, RouteResult }
import akka.stream.QueueOfferResult.{ Dropped, Enqueued, QueueClosed, Failure => OfferFailure }
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ Materializer, OverflowStrategy }
import com.typesafe.config.ConfigFactory

import eu.shiftforward.apso.Logging

/**
 * Adds proxy to akka-http services to proxy requests to other hosts.
 *
 * If the target server is known in advance, a `Proxy` object can be created. This internally materializes a flow that
 * is continuously active and ready to route incoming requests.
 *
 * For one-off requests or requests to previously unknown hosts, this trait defines two routes:
 * - `proxySingleTo` takes the original request and proxies it to the proxy URI;
 * - `proxySingleToUnmatchedPath` copies only the unmatched path from the original URI, and adds it to the path of the
 * proxy URI.
 */
trait ProxySupport extends ClientIPDirectives {

  private[this] def getHeaders(ip: Option[RemoteAddress], headers: List[HttpHeader] = Nil) = {
    // filter `Host` and `Timeout-Access` headers
    val hs = headers.filterNot(header => header.is("host") || header.is("timeout-access"))
    // add `X-Forwarded-For` header
    ip.fold(hs)(addForwardedFor(_, hs))
  }

  private[this] def addForwardedFor(ip: RemoteAddress, headers: List[HttpHeader]): List[HttpHeader] = {
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

  private[this] val optionalRemoteAddress: Directive1[Option[RemoteAddress]] =
    headerValuePF { case `Remote-Address`(address) => Some(address) } | provide(None)

  /**
   * Proxies a single request to a destination URI.
   *
   * @param uri the target URI
   * @return a route that handles requests by proxying them to the given URI.
   */
  def proxySingleTo(uri: Uri): Route = {
    extractActorSystem { implicit system =>
      extractMaterializer { implicit mat =>
        optionalRemoteAddress { ip => ctx =>
          val req = ctx.request.copy(
            uri = uri,
            headers = getHeaders(ip, ctx.request.headers.toList))

          import system.dispatcher
          Http(system).singleRequest(req).map(Complete.apply)
        }
      }
    }
  }

  /**
   * Proxies a single request to a destination base URI. The target URI is created by concatenating the base URI with
   * the unmatched path.
   *
   * @param uri the target base URI
   * @return a route that handles requests by proxying them to the given URI.
   */
  def proxySingleToUnmatchedPath(uri: Uri): Route = {
    extractActorSystem { implicit system =>
      extractMaterializer { implicit mat =>
        optionalRemoteAddress { ip => ctx =>
          val req = ctx.request.copy(
            uri = uri.withPath(uri.path ++ ctx.unmatchedPath).withQuery(ctx.request.uri.query()),
            headers = getHeaders(ip, ctx.request.headers.toList))

          import system.dispatcher
          Http(system).singleRequest(req).map(Complete.apply)
        }
      }
    }
  }

  private[this] lazy val defaultQueueSize =
    ConfigFactory.load.getInt("akka.http.host-connection-pool.max-open-requests")

  /**
   * A representation of a reverse proxy for a remote host. This class internally materializes a flow that is
   * continuously active and ready to route incoming requests.
   *
   * @param host the target host
   * @param port the target port
   * @param reqQueueSize the maximum size of the queue of pending backend requests
   */
  class Proxy(host: String, port: Int, reqQueueSize: Int = defaultQueueSize)(implicit system: ActorSystem, mat: Materializer)
      extends Logging {

    import system.dispatcher

    private[this] lazy val source = Source.queue[(HttpRequest, Promise[RouteResult])](
      reqQueueSize, OverflowStrategy.dropNew)

    private[this] lazy val flow = Http().cachedHostConnectionPool[Promise[RouteResult]](host, port)

    private[this] lazy val sink = Sink.foreach[(Try[HttpResponse], Promise[RouteResult])] {
      case ((Success(resp), p)) => p.success(Complete(resp))
      case ((Failure(e), p)) => p.failure(e)
    }

    private[this] lazy val queue = source.via(flow).toMat(sink)(Keep.left).run()

    /**
     * Proxies a request to a destination URI.
     *
     * @param uri the target URI
     * @return a route that handles requests by proxying them to the given URI.
     */
    def proxyTo(uri: Uri): Route = {
      optionalRemoteAddress { ip => ctx =>
        val req = ctx.request.copy(uri = uri, headers = getHeaders(ip, ctx.request.headers.toList))
        val promise = Promise[RouteResult]()

        queue.offer(req -> promise).flatMap {
          case Enqueued => promise.future
          case OfferFailure(ex) => Future.failed(new RuntimeException("Queue offering failed", ex))
          case QueueClosed => Future.failed(new RuntimeException("Queue is completed before call!?"))
          case Dropped =>
            log.warn("Request queue for {}:{} is full", host, port)
            Future.successful(Complete(HttpResponse(StatusCodes.ServiceUnavailable)))
        }
      }
    }
  }
}
