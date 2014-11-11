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

  def get(req: String): Response = exec(req.GET)
  def post(req: String, body: String): Response = exec(req.POST.setBody(body))
  def put(req: String, body: String): Response = exec(req.PUT.setBody(body))
  def delete(req: String): Response = exec(req.DELETE)
  def head(req: String): Response = exec(req.HEAD)
}
