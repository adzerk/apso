package eu.shiftforward.apso.json

import java.net.URI

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Try

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import org.joda.time.Interval
import spray.http.MediaTypes._
import spray.http._
import spray.json.DefaultJsonProtocol._
import spray.json._
import org.joda.time.DateTime

import eu.shiftforward.apso.config.Implicits._
import eu.shiftforward.apso.json.Implicits._

/**
 * Provides additional JsonFormats not available in the [[spray.json.DefaultJsonProtocol]].
 */
object ExtraJsonProtocol
  extends ExtraTimeJsonProtocol
  with ExtraHttpJsonProtocol
  with ExtraMiscJsonProtocol

trait ExtraTimeJsonProtocol {
  implicit object FiniteDurationJsonFormat extends JsonFormat[FiniteDuration] {
    def write(dur: FiniteDuration) = JsObject("milliseconds" -> dur.toMillis.toJson)

    def read(json: JsValue) = {
      json match {
        case JsString(duration) => ConfigFactory.parseString(s"d=$duration").get[FiniteDuration]("d")
        case j: JsObject =>
          j.fields.headOption match {
            case Some(("milliseconds", JsNumber(milliseconds))) => milliseconds.longValue.millis
            case Some(("seconds", JsNumber(seconds))) => seconds.longValue.seconds
            case Some(("minutes", JsNumber(minutes))) => minutes.longValue.minutes
            case Some(("hours", JsNumber(hours))) => hours.longValue.hours
            case Some(("days", JsNumber(days))) => days.longValue.days
            case _ => deserializationError(
              "Expected the following units: 'milliseconds', 'seconds', 'minutes', 'hours' or 'days'.")
          }
        case _ => deserializationError(
          "Expected the following units: 'milliseconds', 'seconds', 'minutes', 'hours' or 'days'.")
      }
    }
  }

  implicit object IntervalJsonFormat extends JsonFormat[Interval] {
    def write(i: Interval): JsValue =
      JsObject(
        "startMillis" -> i.getStartMillis.toJson,
        "endMillis" -> i.getEndMillis.toJson)

    def read(json: JsValue): Interval = {
      json.asJsObject.getFields("startMillis", "endMillis") match {
        case Seq(startMillis, endMillis) =>
          new Interval(startMillis.convertTo[Long], endMillis.convertTo[Long])
        case _ =>
          deserializationError(
            "One ore more fields are missing or malformed in the Interval Json. " +
              "Required fields: 'startMillis' and 'endMillis'.")
      }
    }
  }
}

trait ExtraHttpJsonProtocol {

  implicit object URIFormat extends JsonFormat[URI] {
    def write(uri: URI) = JsString(uri.toString)

    def read(json: JsValue) = json match {
      case JsString(uri) => new URI(uri)
      case other => deserializationError("Expected String with URI, got: " + other)
    }
  }

  // Spray jsonFormatters:
  implicit object HttpMethodJsonFormat extends JsonFormat[HttpMethod] {
    def read(json: JsValue) = null
    def write(method: HttpMethod) = JsString(method.value)
  }

  implicit object UriHostMethodJsonFormat extends JsonFormat[Uri.Host] {
    def read(json: JsValue) = null
    def write(host: Uri.Host) = JsString(host.address)
  }

  implicit object UriPathMethodJsonFormat extends JsonFormat[Uri.Path] {
    def read(json: JsValue) = null
    def write(path: Uri.Path) = JsString(path.toString())
  }

  implicit object UriQueryMethodJsonFormat extends JsonFormat[Uri.Query] {

    object JsonPath {
      def unapply(paramKey: String) = Some(paramKey.filter(_ != ']').split("\\["))
    }

    def read(json: JsValue) = null
    def write(query: Uri.Query) = {
      val untypedJs = mutable.Map.empty[String, Any]

      query.foreach {
        case (JsonPath(path), value) =>
          val obj = path.view(0, path.length - 1).foldLeft(untypedJs) { (curr, elem) =>
            curr.getOrElseUpdate(elem, mutable.Map()).asInstanceOf[mutable.Map[String, Any]]
          }
          obj.update(path(path.length - 1), value)
      }

      def toJson(obj: Any): JsValue = obj match {
        case str: String => JsString(str)

        case obj: mutable.Map[_, _] if Try(obj.head._1.toString.toInt).isSuccess =>
          val map = obj.asInstanceOf[mutable.Map[String, Any]]
          val arr = Array.ofDim[JsValue](map.keysIterator.map(_.toInt).max + 1)

          for ((ToInt(i), value) <- obj) arr(i) = toJson(value)
          arr.toJson

        case obj: mutable.Map[_, _] =>
          obj.asInstanceOf[mutable.Map[String, Any]].mapValues(toJson).toMap.toJson
      }

      toJson(untypedJs)
    }
  }

  implicit val authorityJsonFormat = jsonFormat3(Uri.Authority.apply)

  implicit object UriJsonFormat extends JsonFormat[Uri] {
    val jf = jsonFormat5(Uri.apply)

    def read(json: JsValue) = null
    def write(uri: Uri) = JsObject(
      jf.write(uri).asJsObject.fields + ("full" -> JsString(uri.toString())))
  }

  implicit object HttpCookiesJsonFormat extends JsonFormat[Seq[HttpCookie]] {
    def read(json: JsValue) = null

    def write(cookies: Seq[HttpCookie]) = cookies.foldRight(Map.empty[String, JsValue]) {
      case (cookie, acc) =>
        acc.get(cookie.name) match {
          case None => acc.updated(cookie.name, JsString(cookie.content))

          case Some(jStr: JsString) =>
            acc.updated(cookie.name, JsArray(JsString(cookie.content), jStr))

          case Some(JsArray(vals)) =>
            acc.updated(cookie.name, JsArray(JsString(cookie.content) +: vals))

          case e => throw new AssertionError(e + " not expected")
        }
    }.toJson
  }

  implicit object HttpHeadersJsonFormat extends JsonFormat[List[HttpHeader]] {
    def read(json: JsValue) = null
    def write(headers: List[HttpHeader]) = headers.foldRight(Map.empty[String, JsValue]) {

      case (HttpHeaders.Cookie(cookies), acc) =>
        acc.get("Cookie") match {
          case None => acc.updated("Cookie", cookies.toJson)
          case Some(JsObject(fields)) => acc.updated("Cookie", cookies.toJson) // TODO merge with existing JSON
          case e => throw new AssertionError(e + " not expected")
        }

      case (header, acc) =>
        acc.get(header.name) match {
          case None => acc.updated(header.name, JsString(header.value))

          case Some(JsString(str)) =>
            acc.updated(header.name, JsString(header.value + "," + str))

          case e => throw new AssertionError(e + " not expected")
        }

    }.toJson
  }

  implicit object HttpProtocolJsonFormat extends JsonFormat[HttpProtocol] {
    def read(json: JsValue) = null
    def write(protocol: HttpProtocol) = JsString(protocol.value)
  }

  implicit object HttpEntityJsonFormat extends JsonFormat[HttpEntity] {
    def read(json: JsValue) = null
    def write(entity: HttpEntity) = entity match {
      case HttpEntity.Empty => JsNull
      case HttpEntity.NonEmpty(contentType, data) =>
        if (contentType.mediaType == `application/json`) data.asString.parseJson
        else JsString(data.asString)
    }
  }

  implicit val httpRequestJsonFormat = new RootJsonFormat[HttpRequest] {
    val defaultFormat = jsonFormat5(HttpRequest.apply)

    override def write(obj: HttpRequest): JsValue = {
      val defaultJson = obj.toJson(defaultFormat).asJsObject
      val query = obj.uri.query.toJson match {
        case obj: JsObject => obj
        case _ => JsObject.empty
      }
      val body = obj.entity.toJson match {
        case obj: JsObject => obj
        case _ => JsObject.empty
      }
      val data = JsObject(query.fields ++ body.fields)

      JsObject(defaultJson.fields ++ Map("data" -> data))
    }

    override def read(json: JsValue): HttpRequest = json.convertTo[HttpRequest](defaultFormat)
  }
}

trait ExtraMiscJsonProtocol {
  implicit object ConfigJsonFormat extends JsonFormat[Config] {
    def write(conf: Config): JsValue = conf.root.render(ConfigRenderOptions.concise()).parseJson
    def read(json: JsValue): Config = ConfigFactory.parseString(json.toString)
  }

  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    override def write(date: DateTime): JsValue = JsString(date.toString)

    override def read(json: JsValue): DateTime = json match {
      case JsString(date) => new DateTime(date)
      case _ =>
        deserializationError("The value for a 'DateTime' has an invalid type - it must be a String.")
    }
  }
}
