package eu.shiftforward.apso.http

import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object W {
  private[this] lazy val log = LoggerFactory.getLogger("W")

  implicit def stringAsReq(str: String) = url(str)

  def exec(req: Req) = {
    log.debug("{} {}", req.toRequest.getMethod, req.toRequest.getUrl, null)
    Await.result(Http(req), 3.seconds)
  }

  def get(req: String, headers: Map[String, Seq[String]] = Map()): Response =
    exec(req.GET.setHeaders(headers))
  def post(req: String, body: String, headers: Map[String, Seq[String]] = Map()): Response =
    exec(req.POST.setBody(body).setHeaders(headers))
  def put(req: String, body: String, headers: Map[String, Seq[String]] = Map()): Response =
    exec(req.PUT.setBody(body).setHeaders(headers))
  def delete(req: String, headers: Map[String, Seq[String]] = Map()): Response =
    exec(req.DELETE.setHeaders(headers))
  def head(req: String, headers: Map[String, Seq[String]] = Map()): Response =
    exec(req.HEAD.setHeaders(headers))
}
