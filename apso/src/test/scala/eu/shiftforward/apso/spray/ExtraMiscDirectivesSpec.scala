package eu.shiftforward.apso.spray

import org.specs2.mutable.Specification
import spray.http.CacheDirectives.{ `max-age`, `must-revalidate`, `no-cache`, `no-store` }
import spray.http.HttpHeaders.`Cache-Control`
import spray.routing._
import spray.http.StatusCodes.OK
import spray.testkit.Specs2RouteTest

class ExtraMiscDirectivesSpec extends Specification with Directives with Specs2RouteTest {

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
  }
}
