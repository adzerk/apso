package com.kevel.apso.pekko.http

import java.net.InetAddress

import scala.concurrent.duration.*
import scala.concurrent.{Future, Promise}

import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.RemoteAddress.IP
import org.apache.pekko.http.scaladsl.model.StatusCodes.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.headers.*
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.RouteResult
import org.apache.pekko.http.scaladsl.server.RouteResult.Complete
import org.apache.pekko.http.scaladsl.testkit.{RouteTestTimeout, Specs2RouteTest}
import org.apache.pekko.stream.scaladsl.Flow
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import com.kevel.apso.NetUtils.*

class ProxySupportSpec(implicit ee: ExecutionEnv) extends Specification with Specs2RouteTest with ProxySupport {

  trait MockServer extends Scope {
    def serverResponse(req: HttpRequest): Future[HttpResponse] =
      Future.successful(HttpResponse(entity = req.uri.toRelative.toString))

    val (interface, port) = ("localhost", availablePort())

    // Server that replies with the request relative URI and ignores DELETE requests
    val boundFuture = {
      val serverFlow: Flow[HttpRequest, HttpResponse, NotUsed] = Flow
        .apply[HttpRequest]
        .filter(_.method != HttpMethods.DELETE)
        .mapAsync(1)(serverResponse)
      Http().newServerAt(interface, port).bindFlow(serverFlow)
    }

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

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds)

  "An pekko-http proxy support directive" should {

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
    }

    "respect the failOnDrop flag" in new MockServer {
      def parseResult(result: RouteResult): Future[String] = result match {
        case Complete(res) if res.status.intValue() == 200 =>
          res.entity.toStrict(10.seconds).map { r => r.data.utf8String }
        case Complete(res) => Future.successful(res.status.intValue().toString)
        case _             => Future.failed(new Exception("Failed to parse result"))
      }

      val latch = Promise[Unit]()

      override def serverResponse(req: HttpRequest): Future[HttpResponse] =
        latch.future.flatMap(_ => super.serverResponse(req))

      val badProxy = new Proxy(interface, port, reqQueueSize = 1)
      // Fill the bad proxy. We're trying to fill the maximum number of available connections here.
      (0 to 300).foreach(x => badProxy.sendRequest(Get(s"/proxied-$x"), failOnDrop = false))

      badProxy.sendRequest(Get("/proxied"), failOnDrop = true).failed.map(_.getMessage) must be_==(
        "Dropping request (Queue is full)"
      ).awaitFor(10.seconds)

      badProxy
        .sendRequest(Get("/proxied"), failOnDrop = false)
        .flatMap(parseResult) must be_==("503").awaitFor(10.seconds)

      latch.success(())

      proxy
        .sendRequest(Get("/proxied"), failOnDrop = false)
        .flatMap(parseResult) must be_==("/proxied").awaitFor(10.seconds)
    }

    "do not send unwanted headers" in new MockServer {
      override def serverResponse(req: HttpRequest) =
        Future.successful(
          HttpResponse(entity = req.headers.map(header => s"${header.name}: ${header.value}").mkString("\n"))
        )
      Get("/get-path-proxied").withHeaders(
        Host("expecteddomain.com"),
        `Raw-Request-URI`("somedomain.com")
      ) ~> routes ~> check {
        responseAs[String] must not(contain("Remote-Address"))
        responseAs[String] must not(contain("Raw-Request-URI"))
        responseAs[String] must contain("Host: expecteddomain.com")
      }
    }

    "Modify the `X-Forwarded-For` header" in {
      trait CollectHeadersAndForwardedForMockServer extends MockServer {
        override def serverResponse(req: HttpRequest) = {
          val forwardedForIps = req.headers
            .collectFirst { case `X-Forwarded-For`(ips) =>
              ips
            }
            .getOrElse(Seq.empty)
          Future.successful(HttpResponse(entity = forwardedForIps.mkString(", "), headers = req.headers))
        }
      }

      "add `X-Forwarded-For` if request has `Remote-Address`" in new CollectHeadersAndForwardedForMockServer {
        Get("/get-path-proxied").withAttributes(Map(AttributeKeys.remoteAddress -> localIp1)) ~> routes ~> check {
          responseAs[String] must be_==("127.0.0.1")
        }
      }

      "update existing `X-Forwarded-For`" in new CollectHeadersAndForwardedForMockServer {
        Get("/get-path-proxied")
          .withHeaders(`X-Forwarded-For`(localIp2))
          .withAttributes(Map(AttributeKeys.remoteAddress -> localIp1)) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.2, 127.0.0.1")
        }

        Get("/get-path-proxied").withHeaders(`X-Forwarded-For`(localIp2)) ~> routes ~> check {
          status == OK
          responseAs[String] must be_==("127.0.0.2")
        }
      }

      "do not add `X-Forwarded-For` if no `Remote-Address`" in new CollectHeadersAndForwardedForMockServer {
        Get("/get-path-proxied") ~> routes ~> check {
          status == OK
          responseAs[String] must beEmpty
        }
      }
    }
  }
}
