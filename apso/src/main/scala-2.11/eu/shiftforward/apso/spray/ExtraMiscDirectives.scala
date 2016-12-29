package eu.shiftforward.apso.spray

import java.net.URL

import scala.util.Try

import spray.http.CacheDirectives.{ `max-age`, `must-revalidate`, `no-cache`, `no-store` }
import spray.http.HttpHeaders.`Cache-Control`
import spray.routing._
import spray.routing.directives.RespondWithDirectives
import spray.routing.directives.HeaderDirectives._

/**
 * Exposes additional misc directives not present in [[spray.routing.directives.MiscDirectives]].
 */
trait ExtraMiscDirectives {

  def cacheControlMaxAge(inMinutes: Option[Long]): Directive0 =
    inMinutes match {
      case None =>
        ExtraMiscDirectives.cacheControlNoCache
      case Some(s) =>
        RespondWithDirectives.respondWithSingletonHeader(`Cache-Control`(`max-age`(60l * s), `must-revalidate`))
    }

  /**
   * Extracts a valid Referer host from the HTTP request headers
   */
  def optionalRefererHost: Directive1[Option[String]] =
    optionalHeaderValueByName("referer")
      .map(_.flatMap(r => Try(new URL(r)).toOption.map(_.getHost)))
}

object ExtraMiscDirectives extends ExtraMiscDirectives {
  final val cacheControlNoCache: Directive0 =
    RespondWithDirectives.respondWithSingletonHeader(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))
}
