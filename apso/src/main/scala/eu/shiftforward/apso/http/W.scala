package eu.shiftforward.apso.http

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import com.mashape.unirest.http.{ HttpResponse, Unirest }
import com.mashape.unirest.request.{ HttpRequest, HttpRequestWithBody }
import org.slf4j.LoggerFactory

object W {
  case class Timeout(duration: FiniteDuration)
  private[this] lazy val log = LoggerFactory.getLogger("W")
  private[this] lazy val defaultTimeout = Timeout(10.seconds)

  private[this] def javaHeaders(headers: Map[String, Seq[String]]) =
    headers.flatMap { case (k, vs) => vs.map(k -> _) }.asJava

  private[this] def exec(req: HttpRequest, timeout: FiniteDuration) = {
    log.debug("{} {}", req.getHttpMethod, req.getUrl, null)
    req.asStringAsync().get(timeout.length, timeout.unit)
  }

  private[this] def exec(req: HttpRequestWithBody, body: String, timeout: FiniteDuration) = {
    log.debug("{} {}", req.getHttpMethod, req.getUrl, null)
    req.body(body).asStringAsync().get(timeout.length, timeout.unit)
  }

  def get(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    exec(Unirest.get(req).headers(javaHeaders(headers)), timeout.duration)
  def post(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    exec(Unirest.post(req).headers(javaHeaders(headers)), body, timeout.duration)
  def put(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    exec(Unirest.put(req).headers(javaHeaders(headers)), body, timeout.duration)
  def delete(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    exec(Unirest.delete(req).headers(javaHeaders(headers)), timeout.duration)
  def head(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): HttpResponse[String] =
    exec(Unirest.head(req).headers(javaHeaders(headers)), timeout.duration)
}
