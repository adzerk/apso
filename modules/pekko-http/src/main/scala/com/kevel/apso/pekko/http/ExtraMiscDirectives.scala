package com.kevel.apso.pekko.http

import java.net.URI

import scala.concurrent.duration.*
import scala.util.Try

import org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.*
import org.apache.pekko.http.scaladsl.model.headers.*
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.{Directive0, Directive1}

/** Exposes additional misc directives not present in
  * [[org.apache.pekko.http.scaladsl.server.directives.MiscDirectives]].
  */
trait ExtraMiscDirectives {

  final val cacheControlNoCache: Directive0 =
    respondWithDefaultHeader(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))

  /** Inserts a "Cache-Control" header, instructing the browser to cache the HTTP response for the supplied duration.
    * The header key "max-age" specifies the number of seconds during which the browser should cache the HTTP response.
    * In case the supplied duration is less than 1 second, this directive defaults to the minimum duration allowed, 1
    * second.
    *
    * @param maxAgeDuration
    *   the duration for how long to cache the HTTP response
    * @return
    *   a Directive that inserts a Cache-Control header
    */
  def cacheControlMaxAge(maxAgeDuration: Option[FiniteDuration]): Directive0 = {
    maxAgeDuration match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        respondWithDefaultHeader(`Cache-Control`(`max-age`(math.max(s.toSeconds, 1)), `must-revalidate`))
    }
  }

  /** Extracts a valid Referer host from the HTTP request headers
    */
  def optionalRefererHost: Directive1[Option[String]] =
    optionalHeaderValueByName("referer")
      .map(_.flatMap(r => Try(new URI(r)).toOption.map(_.getHost)))
}

object ExtraMiscDirectives extends ExtraMiscDirectives
