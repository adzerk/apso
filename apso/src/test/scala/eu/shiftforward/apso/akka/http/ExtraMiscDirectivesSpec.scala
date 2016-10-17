package eu.shiftforward.apso.akka.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.specs2.mutable.Specification

class ExtraMiscDirectivesSpec extends Specification with Specs2RouteTest {

  "The ExtraMiscDirectives" should {
    import ExtraMiscDirectives._

    def noCacheRoute: Route = cacheControlNoCache(complete(OK))

    def maxAgeRoute(age: Option[Long]): Route = cacheControlMaxAge(age)(complete(OK))

    "expose a no-cache control directive" in {
      Get("/") ~> noCacheRoute ~> check {
        response.headers must contain(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))
      }
    }

    "expose a max-age directive" in {
      Get("/") ~> maxAgeRoute(Some(9001l)) ~> check {
        response.headers must contain(`Cache-Control`(`max-age`(60l * 9001), `must-revalidate`))
      }

      Get("/") ~> maxAgeRoute(None) ~> check {
        response.headers must contain(`Cache-Control`(`no-cache`, `no-store`, `must-revalidate`))
      }
    }

    "expose an optional referer header host directive" in {
      val optRefRoute: Route =
        get {
          optionalRefererHost { referer =>
            complete(referer.toString)
          }
        }

      Get("/") ~> optRefRoute ~> check {
        responseAs[String] mustEqual None.toString
      }

      Get("/") ~> addHeader(RawHeader("Referer", "http://example.com")) ~> optRefRoute ~> check {
        responseAs[String] mustEqual Some("example.com").toString
      }

      Get("/") ~> addHeader(RawHeader("Referer", "invalidhost :(")) ~> optRefRoute ~> check {
        responseAs[String] mustEqual None.toString
      }
    }
  }
}
