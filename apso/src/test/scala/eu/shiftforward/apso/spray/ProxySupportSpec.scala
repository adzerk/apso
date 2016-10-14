package eu.shiftforward.apso.spray

import scala.concurrent.duration._
import akka.actor.{ Actor, Props }
import akka.io.IO
import akka.pattern.ask
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.can.Http
import spray.http.StatusCodes._
import spray.http.{ HttpRequest, HttpResponse, Uri }
import spray.routing._
import spray.testkit.Specs2RouteTest
import spray.util._
import spray.http.HttpHeaders._

class ProxySupportSpec
    extends Specification
    with Directives
    with Specs2RouteTest
    with ProxySupport {

  trait MockServer extends Scope {

    def serverPath = "remote-proxy"
    def serverResponse(req: HttpRequest): HttpResponse = {
      HttpResponse(entity = req.uri.toRelative.toString)
    }

    val (interface, port) = Utils.temporaryServerHostnameAndPort()

    val testService = system.actorOf {
      Props {
        new Actor {
          def receive = {
            case x: Http.Connected => sender ! Http.Register(self)
            case req: HttpRequest => sender ! serverResponse(req)
            case _: Http.ConnectionClosed => // ignore
          }
        }
      }
    }
    IO(Http).ask(Http.Bind(testService, interface, port))(10.seconds).await

    def routes = {
      // format: OFF
      (get | post) {
        path("get-path") {
          complete("get-reply")
        } ~
        path("get-path-proxied") {
          proxyTo(Uri(s"http://$interface:$port/$serverPath"))
        } ~
        pathPrefix("get-path-proxied-unmatched") {
          proxyToUnmatchedPath(Uri(s"http://$interface:$port/$serverPath"))
        }
      }
      // format: ON
    }
  }

  implicit val timeout = RouteTestTimeout(5.seconds)

  "A spray proxy support directive" should {

    "proxy requests" in new MockServer {
      Get("/get-path") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("get-reply")
      }
      Get("/get-path-proxied") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
      Post("/get-path-proxied") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
    }

    "proxy requests using unmatched path" in new MockServer {
      Get("/get-path") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("get-reply")
      }
      Get("/get-path-proxied-unmatched/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Post("/get-path-proxied-unmatched/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Get("/get-path-proxied-unmatched/other/path/parts?foo=bar") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts?foo=bar")
      }
    }

    "Modify the `X-Forwarded-For` header" in new MockServer {

      override def serverResponse(req: HttpRequest) = {
        val forwardedForIps = req.headers.collectFirst {
          case `X-Forwarded-For`(ips) => ips
        }.getOrElse(Seq.empty)
        HttpResponse(entity = forwardedForIps.mkString(", "))
      }

      "add `X-Forwarded-For` if request has `Remote-Address`" in {
        Get("/get-path-proxied").withHeaders(`Remote-Address`("127.0.0.1")) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.1")
        }
      }

      "update existing `X-Forwarded-For`" in {
        Get("/get-path-proxied").withHeaders(`Remote-Address`("127.0.0.1"), `X-Forwarded-For`("127.0.0.2")) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.2, 127.0.0.1")
        }

        Get("/get-path-proxied").withHeaders(`X-Forwarded-For`("127.0.0.2")) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.2")
        }
      }

      "do not add `X-Forwarded-For` if no `Remote-Address`" in {
        Get("/get-path-proxied") ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("a")
        }
      }
    }

  }

}
