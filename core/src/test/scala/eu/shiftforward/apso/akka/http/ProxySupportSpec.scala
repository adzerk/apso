package eu.shiftforward.apso.akka.http

import java.net.{ InetAddress, ServerSocket }
import java.nio.charset.Charset

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.RemoteAddress.IP
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.scaladsl.Flow
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ProxySupportSpec(implicit ee: ExecutionEnv) extends Specification with Specs2RouteTest with ProxySupport {

  trait MockServer extends Scope {
    def serverResponse(req: HttpRequest) = HttpResponse(entity = req.uri.toRelative.toString)

    private[this] def availablePort(): Int = {
      val socket = new ServerSocket(0)
      val port = socket.getLocalPort
      socket.close()
      port
    }

    val (interface, port) = ("localhost", availablePort())
    val boundFuture = Http().bindAndHandle(Flow.fromFunction(serverResponse), interface, port)

    val proxy = new Proxy(interface, port)
    val strictProxy = new Proxy(interface, port, strictTimeout = Some(10.seconds))

    val routes = {
      // format: OFF
      (get | post) {
        path("get-path") {
          complete("get-reply")
        } ~
        path("get-path-proxied-single-strict") {
          strictProxySingleTo(Uri(s"http://$interface:$port/remote-proxy"), 10.seconds)
        } ~
        path("get-path-proxied-single") {
          proxySingleTo(Uri(s"http://$interface:$port/remote-proxy"))
        } ~
        pathPrefix("get-path-proxied-single-unmatched-strict") {
          strictProxySingleToUnmatchedPath(Uri(s"http://$interface:$port/remote-proxy"), 10.seconds)
        } ~
        pathPrefix("get-path-proxied-single-unmatched") {
          proxySingleToUnmatchedPath(Uri(s"http://$interface:$port/remote-proxy"))
        } ~
        pathPrefix("get-path-proxied-strict") {
          strictProxy.proxyTo(Uri(s"http://$interface:$port/remote-proxy"))
        } ~
        pathPrefix("get-path-proxied") {
          proxy.proxyTo(Uri(s"http://$interface:$port/remote-proxy"))
        }
      }
      // format: ON
    }

    boundFuture.map(_.localAddress.isUnresolved) must beFalse.awaitFor(10.seconds)
  }

  val localIp1 = IP(InetAddress.getByName("127.0.0.1"))
  val localIp2 = IP(InetAddress.getByName("127.0.0.2"))

  implicit val timeout = RouteTestTimeout(5.seconds)

  "An akka-http proxy support directive" should {

    "proxy single requests" in new MockServer {
      Get("/get-path") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("get-reply")
      }

      Get("/get-path-proxied-single") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
      Post("/get-path-proxied-single") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }

      Get("/get-path-proxied-single-strict") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
      Post("/get-path-proxied-single-strict") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
    }

    "proxy single requests using unmatched path" in new MockServer {
      Get("/get-path") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("get-reply")
      }

      Get("/get-path-proxied-single-unmatched/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Post("/get-path-proxied-single-unmatched/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Get("/get-path-proxied-single-unmatched/other/path/parts?foo=bar") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts?foo=bar")
      }

      Get("/get-path-proxied-single-unmatched-strict/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Post("/get-path-proxied-single-unmatched-strict/other/path/parts") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts")
      }
      Get("/get-path-proxied-single-unmatched-strict/other/path/parts?foo=bar") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy/other/path/parts?foo=bar")
      }
    }

    "proxy requests using a dedicated Proxy object" in new MockServer {
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

      Get("/get-path-proxied-strict") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }
      Post("/get-path-proxied-strict") ~> routes ~> check {
        status == OK
        responseAs[String] must be_==("/remote-proxy")
      }

      proxy.sendRequest(Get("/proxied"), failOnDrop = false).flatMap {
        case Complete(res) => res.entity.toStrict(10.seconds).map { r =>
          val charset = r.contentType.charsetOption.map(_.nioCharset()).getOrElse(Charset.forName("UTF-8"))
          r.data.decodeString(charset)
        }
        case _ => Future.failed(new Exception("Failed to parse result"))
      } must be_==("/proxied").awaitFor(10.seconds)
    }

    "Modify the `X-Forwarded-For` header" in new MockServer {

      override def serverResponse(req: HttpRequest) = {
        val forwardedForIps = req.headers.collectFirst {
          case `X-Forwarded-For`(ips) => ips
        }.getOrElse(Seq.empty)
        HttpResponse(entity = forwardedForIps.mkString(", "))
      }

      "add `X-Forwarded-For` if request has `Remote-Address`" in {
        Get("/get-path-proxied").withHeaders(`Remote-Address`(localIp1)) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.1")
        }
      }

      "update existing `X-Forwarded-For`" in {
        Get("/get-path-proxied").withHeaders(`Remote-Address`(localIp1), `X-Forwarded-For`(localIp2)) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.2, 127.0.0.1")
        }

        Get("/get-path-proxied").withHeaders(`X-Forwarded-For`(localIp2)) ~> routes ~> check {
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
