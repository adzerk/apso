package eu.shiftforward.apso.http

import org.asynchttpclient._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.collection.JavaConverters._

object W {
  case class Timeout(duration: FiniteDuration)
  private[this] lazy val log = LoggerFactory.getLogger("W")
  private[this] lazy val defaultTimeout = Timeout(10.seconds)

  private[this] val client = new DefaultAsyncHttpClient
  private[this] def javaHeaders(headers: Map[String, Seq[String]]) = headers.mapValues(_.asJava).asJava

  implicit private[this] def reqBuilderAsReq(builder: RequestBuilderBase[_]) = builder.build()

  def exec(req: Request, timeout: FiniteDuration = defaultTimeout.duration) = {
    log.debug("{} {}", req.getMethod, req.getUrl, null)
    client.executeRequest(req).get(timeout.length, timeout.unit)
  }

  def get(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(client.prepareGet(req).setHeaders(javaHeaders(headers)), timeout.duration)
  def post(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(client.preparePost(req).setBody(body).setHeaders(javaHeaders(headers)), timeout.duration)
  def put(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(client.preparePut(req).setBody(body).setHeaders(javaHeaders(headers)), timeout.duration)
  def delete(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(client.prepareDelete(req).setHeaders(javaHeaders(headers)), timeout.duration)
  def head(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(client.prepareHead(req).setHeaders(javaHeaders(headers)), timeout.duration)
}
