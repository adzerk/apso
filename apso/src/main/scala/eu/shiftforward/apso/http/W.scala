package eu.shiftforward.apso.http

import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object W {
  case class Timeout(duration: FiniteDuration)
  private[this] lazy val log = LoggerFactory.getLogger("W")
  private[this] lazy val defaultTimeout = Timeout(10.seconds)

  implicit def stringAsReq(str: String) = url(str)

  def exec(req: Req, timeout: FiniteDuration = defaultTimeout.duration) = {
    log.debug("{} {}", req.toRequest.getMethod, req.toRequest.getUrl, null)
    Await.result(Http(req), timeout)
  }

  def get(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(req.GET.setHeaders(headers), timeout.duration)
  def post(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(req.POST.setBody(body).setHeaders(headers), timeout.duration)
  def put(req: String, body: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(req.PUT.setBody(body).setHeaders(headers), timeout.duration)
  def delete(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(req.DELETE.setHeaders(headers), timeout.duration)
  def head(req: String, headers: Map[String, Seq[String]] = Map())(implicit timeout: Timeout = defaultTimeout): Response =
    exec(req.HEAD.setHeaders(headers), timeout.duration)
}
