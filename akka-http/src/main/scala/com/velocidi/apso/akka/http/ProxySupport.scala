package com.velocidi.apso.akka.http

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{Directive1, RequestContext, Route, RouteResult}
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure => OfferFailure, QueueClosed}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory

import com.velocidi.apso.Logging

/** Adds proxy to akka-http services to proxy requests to other hosts.
  *
  * If the target server is known in advance, a `Proxy` object can be created. This internally materializes a flow that
  * is continuously active and ready to route incoming requests.
  *
  * For one-off requests or requests to previously unknown hosts, this trait defines two routes:
  *   - `proxySingleTo` takes the original request and proxies it to the proxy URI;
  *   - `proxySingleToUnmatchedPath` copies only the unmatched path from the original URI, and adds it to the path of
  *     the proxy URI.
  *
  * In order for the client ip to be propagated in `X-Forwarded-For` headers, the
  * `akka.http.server.remote-address-attribute` config setting must be set to "on".
  */
trait ProxySupport extends ClientIPDirectives {

  private[this] def getHeaders(ip: Option[RemoteAddress], headers: List[HttpHeader]) = {
    // FIXME: Properly decide which headers to keep or filter out.
    //        If designing a proper transparent proxy trait, we should take care of which headers
    //        get passed down. It may be worth looking into the RFC.
    //        - https://www.mnot.net/blog/2011/07/11/what_proxies_must_do
    //        - https://tools.ietf.org/html/draft-ietf-httpbis-p1-messaging-14#section-7.1.3
    //        - https://doc.akka.io/docs/akka-http/current/common/http-model.html
    val hs = headers.filter(_.renderInRequests())
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
    extractRequest.map(_.attribute(AttributeKeys.remoteAddress))

  private[this] def proxy(
      strictTimeout: Option[FiniteDuration] = None
  )(reqBuilder: (Option[RemoteAddress], RequestContext) => HttpRequest): Route = {
    extractActorSystem { implicit system =>
      extractMaterializer { implicit mat =>
        optionalRemoteAddress { ip => ctx =>
          val req = reqBuilder(ip, ctx)
          import system.dispatcher
          strictTimeout match {
            case None => Http(system).singleRequest(req).map(Complete.apply)
            case Some(t) =>
              Http(system)
                .singleRequest(req)
                .flatMap(r => r.entity.toStrict(t).map(e => r.withEntity(e)))
                .map(Complete.apply)
          }
        }
      }
    }
  }

  /** Proxies a single request to a destination URI.
    *
    * @param uri
    *   the target URI
    * @return
    *   a route that handles requests by proxying them to the given URI.
    */
  def proxySingleTo(uri: Uri): Route = proxy() { case (ip, ctx) =>
    ctx.request.withUri(uri).withHeaders(getHeaders(ip, ctx.request.headers.toList))
  }

  /** Proxies a single request to a destination base URI. The target URI is created by concatenating the base URI with
    * the unmatched path.
    *
    * @param uri
    *   the target base URI
    * @return
    *   a route that handles requests by proxying them to the given URI.
    */
  def proxySingleToUnmatchedPath(uri: Uri): Route = proxy() { case (ip, ctx) =>
    ctx.request
      .withUri(uri.withPath(uri.path ++ ctx.unmatchedPath).withQuery(ctx.request.uri.query()))
      .withHeaders(getHeaders(ip, ctx.request.headers.toList))
  }

  /** Proxies a single request to a destination URI. The response in not streamed, but converted to a strict entity with
    * a set timeout.
    *
    * @param uri
    *   the target URI
    * @param timeout
    *   maximum time to wait for the full response.
    * @return
    *   a route that handles requests by proxying them to the given URI.
    */
  def strictProxySingleTo(uri: Uri, timeout: FiniteDuration): Route = proxy(Some(timeout)) { case (ip, ctx) =>
    ctx.request.withUri(uri).withHeaders(getHeaders(ip, ctx.request.headers.toList))
  }

  /** Proxies a single request to a destination base URI. The target URI is created by concatenating the base URI with
    * the unmatched path. The response in not streamed, but converted to a strict entity with a set timeout.
    *
    * @param uri
    *   the target base URI
    * @param timeout
    *   maximum time to wait for the full response.
    * @return
    *   a route that handles requests by proxying them to the given URI.
    */
  def strictProxySingleToUnmatchedPath(uri: Uri, timeout: FiniteDuration): Route = proxy(Some(timeout)) {
    case (ip, ctx) =>
      ctx.request
        .withUri(uri.withPath(uri.path ++ ctx.unmatchedPath).withQuery(ctx.request.uri.query()))
        .withHeaders(getHeaders(ip, ctx.request.headers.toList))
  }

  private[this] lazy val defaultQueueSize =
    ConfigFactory.load.getInt("akka.http.host-connection-pool.max-open-requests")

  /** A representation of a reverse proxy for a remote host. This class internally materializes a flow that is
    * continuously active and ready to route incoming requests.
    *
    * @param host
    *   the target host
    * @param port
    *   the target port
    * @param reqQueueSize
    *   the maximum size of the queue of pending backend requests
    * @param strictTimeout
    *   maximum time to wait for the full response.
    */
  class Proxy(
      host: String,
      port: Int,
      reqQueueSize: Int = defaultQueueSize,
      strictTimeout: Option[FiniteDuration] = None
  )(implicit system: ActorSystem, mat: Materializer)
      extends Logging {

    import system.dispatcher

    private[this] lazy val source =
      Source.queue[(HttpRequest, Promise[RouteResult])](reqQueueSize, OverflowStrategy.dropNew)

    private[this] lazy val flow = strictTimeout match {
      case None => Http().cachedHostConnectionPool[Promise[RouteResult]](host, port)
      case Some(t) =>
        Http()
          .cachedHostConnectionPool[Promise[RouteResult]](host, port)
          .flatMapConcat { case (res, p) =>
            if (res.isFailure) Source.single((res, p))
            else {
              val fut = Future
                .fromTry(res)
                .flatMap(r => r.entity.toStrict(t).map(e => r.withEntity(e)))
                .map(Success.apply)
              Source.future(fut).zip(Source.single(p))
            }
          }
    }

    private[this] lazy val sink = Sink.foreach[(Try[HttpResponse], Promise[RouteResult])] {
      case ((Success(resp), p)) => p.success(Complete(resp))
      case ((Failure(e), p))    => p.failure(e)
    }

    private[this] lazy val queue = source.via(flow).toMat(sink)(Keep.left).run()

    /** Sends a manually crafted request to a destination URI.
      *
      * @param req
      *   the HTTP Request
      * @param failOnDrop
      *   if the future should fail when the message is dropped, or complete with a 503
      * @return
      *   the request result.
      */
    def sendRequest(req: HttpRequest, failOnDrop: Boolean): Future[RouteResult] = {
      val promise = Promise[RouteResult]()
      queue.offer(req -> promise).flatMap {
        case Enqueued         => promise.future
        case OfferFailure(ex) => Future.failed(new RuntimeException("Queue offering failed", ex))
        case QueueClosed      => Future.failed(new RuntimeException("Queue is completed before call!?"))
        case Dropped =>
          log.warn(s"Request queue for $host:$port is full")
          if (failOnDrop) Future.failed(new RuntimeException("Dropping request (Queue is full)"))
          else Future.successful(Complete(HttpResponse(StatusCodes.ServiceUnavailable)))
      }
    }

    /** Proxies a request to a destination URI.
      *
      * @param uri
      *   the target URI
      * @return
      *   a route that handles requests by proxying them to the given URI.
      */
    def proxyTo(uri: Uri): Route = {
      optionalRemoteAddress { ip => ctx =>
        val req = ctx.request.withUri(uri).withHeaders(getHeaders(ip, ctx.request.headers.toList))
        sendRequest(req, failOnDrop = false)
      }
    }
  }
}
