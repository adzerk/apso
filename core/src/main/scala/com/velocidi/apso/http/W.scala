package com.velocidi.apso.http

import scala.concurrent.duration._

import com.mashape.unirest.http.{ HttpResponse, Unirest }
import com.mashape.unirest.request.{ HttpRequest, HttpRequestWithBody }
import com.typesafe.scalalogging.Logger
import io.circe.Json
import org.apache.http.client.config.{ CookieSpecs, RequestConfig }
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.protocol.HttpContext

object W {
  case class Timeout(duration: FiniteDuration)

  private[this] lazy val logger = Logger("W")
  private[this] lazy val defaultTimeout = Timeout(10.seconds)

  private[this] object NeverRedirectStrategy extends DefaultRedirectStrategy {
    override def isRedirected(
      request: org.apache.http.HttpRequest,
      response: org.apache.http.HttpResponse,
      context: HttpContext) = false
  }

  private[this] val reqConfig = RequestConfig.custom
    .setCookieSpec(CookieSpecs.STANDARD)
    .build()

  Unirest.setAsyncHttpClient(HttpAsyncClients.custom
    .setDefaultRequestConfig(reqConfig)
    .setRedirectStrategy(NeverRedirectStrategy)
    .disableCookieManagement()
    .build())

  implicit private[this] class RichHttpRequest(val req: HttpRequest) {
    def headers(headers: Map[String, Seq[String]]) = {
      headers.foldLeft(req) {
        case (acc, (k, vs)) =>
          vs.foldLeft(acc) { (acc2, v) => acc2.header(k, v) }
      }
    }

    def exec(timeout: FiniteDuration) = {
      logger.debug(s"${req.getHttpMethod} ${req.getUrl}")
      req.asStringAsync().get(timeout.length, timeout.unit)
    }
  }

  implicit private[this] class RichHttpRequestWithBody(val req: HttpRequestWithBody) {
    def headers(headers: Map[String, Seq[String]]) = {
      headers.foldLeft(req) {
        case (acc, (k, vs)) =>
          vs.foldLeft(acc) { (acc2, v) => acc2.header(k, v) }
      }
    }

    def exec(timeout: FiniteDuration) = {
      logger.debug(s"${req.getHttpMethod} ${req.getUrl}")
      req.asStringAsync().get(timeout.length, timeout.unit)
    }

    def exec(body: String, timeout: FiniteDuration) = {
      logger.debug(s"${req.getHttpMethod} ${req.getUrl}")
      req.body(body).asStringAsync().get(timeout.length, timeout.unit)
    }
  }

  def get(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    Unirest.get(req).headers(headers).exec(timeout.duration)

  def post(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    Unirest.post(req).headers(headers).exec(body, timeout.duration)

  def put(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    Unirest.put(req).headers(headers).exec(body, timeout.duration)

  def delete(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    Unirest.delete(req).headers(headers).exec(timeout.duration)

  def head(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    Unirest.head(req).headers(headers).exec(timeout.duration)

  def postJson(req: String, body: Json, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    post(req, body.noSpaces, headers.updated("Content-Type", Seq("application/json")))

  def putJson(req: String, body: Json, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    put(req, body.noSpaces, headers.updated("Content-Type", Seq("application/json")))
}
