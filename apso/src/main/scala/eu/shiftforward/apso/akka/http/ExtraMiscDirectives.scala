package eu.shiftforward.apso.akka.http

import java.net.URL

import scala.concurrent.duration._
import scala.util.Try

import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Directive1 }

/**
 * Exposes additional misc directives not present in [[spray.routing.directives.MiscDirectives]].
 */
trait ExtraMiscDirectives {

  final val cacheControlNoCache: Directive0 =
    respondWithDefaultHeader(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))

  @deprecated("Use `eu.shiftforward.apso.akka.http.ExtraMiscDirectives.cacheControlMaxAge(Option[FiniteDuration])` " +
    "instead", "2017/07/04")
  def cacheControlMaxAge(inMinutes: Option[Long])(implicit d: DummyImplicit): Directive0 =
    inMinutes match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        respondWithDefaultHeader(`Cache-Control`(`max-age`(60l * s), `must-revalidate`))
    }

  def cacheControlMaxAge(maxAgeDuration: Option[FiniteDuration]): Directive0 = {
    maxAgeDuration match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) if s < 1.second =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        respondWithDefaultHeader(`Cache-Control`(`max-age`(s.toSeconds), `must-revalidate`))
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
