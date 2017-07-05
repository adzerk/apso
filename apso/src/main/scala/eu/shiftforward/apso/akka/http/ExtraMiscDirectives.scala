package eu.shiftforward.apso.akka.http

import java.net.URL

import scala.concurrent.duration._
import scala.util.Try

import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Directive1, RequestContext }

/**
 * Exposes additional misc directives not present in [[spray.routing.directives.MiscDirectives]].
 */
trait ExtraMiscDirectives {

  final val cacheControlNoCache: Directive0 =
    respondWithDefaultHeader(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))

  @deprecated("Use `eu.shiftforward.apso.akka.http.ExtraMiscDirectives.httpCacheFor(Option[FiniteDuration])` " +
    "instead", "2017/07/04")
  def cacheControlMaxAge(inMinutes: Option[Long]): Directive0 =
    inMinutes match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        respondWithDefaultHeader(`Cache-Control`(`max-age`(60l * s), `must-revalidate`))
    }

  /**
   * Inserts a "Cache-Control" header, instructing the browser to cache the HTTP response for the supplied duration.
   * The header key "max-age" specifies the number of seconds during which the browser should cache the HTTP response.
   * In case the supplied duration is less than 1 second, this directive defaults to the minimum duration allowed,
   * 1 second.
   *
   * @param maxAgeDuration the duration for how long to cache the HTTP response
   * @return a Directive that inserts a Cache-Control header
   */
  def httpCacheFor(maxAgeDuration: Option[FiniteDuration]): Directive0 = {
    maxAgeDuration match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        respondWithDefaultHeader(`Cache-Control`(`max-age`(math.max(s.toSeconds, 1.second.toSeconds)), `must-revalidate`))
    }
  }

  /**
   * Extracts a valid Referer host from the HTTP request headers
   */
  def optionalRefererHost: Directive1[Option[String]] =
    optionalHeaderValueByName("referer")
      .map(_.flatMap(r => Try(new URL(r)).toOption.map(_.getHost)))
}

object ExtraMiscDirectives extends ExtraMiscDirectives
