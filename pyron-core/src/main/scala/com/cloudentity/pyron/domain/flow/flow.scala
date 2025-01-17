package com.cloudentity.pyron.domain.flow

import java.net.URL

import io.circe.Json
import com.cloudentity.pyron.domain.http.{ApiResponse, OriginalRequest, TargetRequest}
import com.cloudentity.tools.vertx.tracing.TracingContext
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.streams.ReadStream

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.matching.Regex

case class PathPrefix(value: String) extends AnyVal
case class PathPattern(value: String) extends AnyVal
case class RewritePath(value: String) extends AnyVal
case class RewriteMethod(value: HttpMethod) extends AnyVal

case class PathParamName(value: String) extends AnyVal

case class PathParams(value: Map[String, String]) extends AnyVal
object PathParams {
  def empty = PathParams(Map())
}

case class PathMatching(regex: Regex, paramNames: List[PathParamName], prefix: PathPrefix, originalPath: String)

object PathMatching {
  val paramNamePlaceholderPattern = """\{(\w+[^/])\}"""
  val paramNamePlaceholderRegex = paramNamePlaceholderPattern.r

  def build(pathPrefix: PathPrefix, pathPattern: PathPattern): PathMatching =
    PathMatching(
      regex = createPatternRegex(createRawPattern(pathPrefix, pathPattern)),
      paramNames = extractPathParamNames(pathPattern),
      prefix = pathPrefix,
      originalPath = pathPattern.value
    )

  def createRawPattern(pathPrefix: PathPrefix, pathPattern: PathPattern): String =
    pathPrefix.value + pathPattern.value

  def createPatternRegex(rawPattern: String): Regex = {
    // capture group 1 contains param name without surrounding parens
    val regex = paramNamePlaceholderRegex.replaceAllIn(rawPattern, m => s"(?<${m.group(1)}>[^/]+)")
    s"^$regex$$".r
  }

  def extractPathParamNames(pattern: PathPattern): List[PathParamName] = {
    paramNamePlaceholderRegex.findAllMatchIn(pattern.value).map(m => PathParamName(m.group(1))).toList
  }

}

case class BasePath(value: String) extends AnyVal
case class DomainPattern(value: String) {
  lazy val regex = new Regex("^" + value.replace("*", "[^\\.]+") + "$")
}

case class GroupMatchCriteria(basePath: Option[BasePath], domains: Option[List[DomainPattern]]) {
  lazy val basePathResolved: BasePath = basePath.getOrElse(BasePath(""))
  lazy val domainsResolved: List[DomainPattern] = domains.getOrElse(Nil)
}

object GroupMatchCriteria {
  val empty = GroupMatchCriteria(None, None)
}

case class EndpointMatchCriteria(method: HttpMethod, path: PathMatching)
case class TargetHost(value: String) extends AnyVal
case class ServiceClientName(value: String) extends AnyVal

sealed trait TargetService
  case class StaticService(host: TargetHost, port: Int, ssl: Boolean) extends TargetService
  case class DiscoverableService(serviceName: ServiceClientName) extends TargetService

object TargetService {
  def apply(rule: TargetServiceRule, req: HttpServerRequest): TargetService =
    rule match {
      case StaticServiceRule(host, port, ssl) => StaticService(host, port, ssl)
      case DiscoverableServiceRule(serviceName) => DiscoverableService(serviceName)
      case ProxyServiceRule => readStaticService(req)
    }

  def Static(host: TargetHost, port: Int, ssl: Boolean): TargetService =
    StaticService(host, port, ssl)

  def discoverable(serviceName: ServiceClientName): TargetService =
    DiscoverableService(serviceName)

  private def readStaticService(req: HttpServerRequest): StaticService = {
    val ssl = req.isSSL
    if (Option(req.host()).isDefined) {
      Option(req.host()).get.split(':').toList match {
        case h :: Nil =>      StaticService(TargetHost(h), 80, ssl)
        case h :: p :: Nil => StaticService(TargetHost(h), Integer.parseInt(p), ssl)
        case _ =>             StaticService(TargetHost("malformed-host-header"), 80, ssl)
      }
    } else Try(new URL(req.absoluteURI())).map(url => {
      val port = if (url.getPort == -1) 80 else url.getPort
      StaticService(TargetHost(url.getHost), port, ssl)
    })
      // it should never fail since you can't create HttpServerRequest with invalid URI
      .toOption.getOrElse(StaticService(TargetHost("malformed-url"), 80, ssl))
  }
}

sealed trait TargetServiceRule
  case class StaticServiceRule(host: TargetHost, port: Int, ssl: Boolean) extends TargetServiceRule
  case class DiscoverableServiceRule(serviceName: ServiceClientName) extends TargetServiceRule
  case object ProxyServiceRule extends TargetServiceRule

trait PluginsConf {
  def pre: List[ApiGroupPluginConf]
  def endpoint: List[ApiGroupPluginConf]
  def post: List[ApiGroupPluginConf]

  def toList = pre ::: endpoint ::: post
}

object Properties {
  def apply(ps: (String, Any)*): Properties = Properties(Map(ps:_*))
}

case class Properties(private val ps: Map[String, Any]) {
  def toMap(): Map[String, Any] = ps

  def updated(key: String, value: Any): Properties =
    Properties(ps.updated(key, value))

  def get[A](key: String): Option[A] =
    ps.get(key).flatMap { value => Try(value.asInstanceOf[A]).toOption }
}

case class AuthnCtx(value: Map[String, Json]) extends AnyVal {
  def apply(v: Map[String, Json]) = AuthnCtx(v)

  def modify(f: Map[String, Json] => Map[String, Json]) =
    apply(f(value))

  def get(name: String): Option[Json] =
    value.get(name)

  def updated(name: String, json: Json) =
    apply(value.updated(name, json))

  def remove(name: String) =
    apply(value - name)

  def merge(other: AuthnCtx) =
    apply(value ++ other.value)

  def mergeMap(other: Map[String, Json]) =
    apply(value ++ other)
}

object AuthnCtx {
  val TOKEN_TYPE = "tokenType"
  def apply(cs: (String, Json)*): AuthnCtx = AuthnCtx(cs.toMap)
}

case class AccessLogItems(value: Map[String, Json]) extends AnyVal {
  def apply(v: Map[String, Json]) = AccessLogItems(v)

  def modify(f: Map[String, Json] => Map[String, Json]) =
    apply(f(value))

  def get(name: String): Option[Json] =
    value.get(name)

  def updated(name: String, json: Json) =
    apply(value.updated(name, json))

  def remove(name: String) =
    apply(value - name)

  def merge(other: AccessLogItems) =
    apply(value ++ other.value)

  def mergeMap(other: Map[String, Json]) =
    apply(value ++ other)
}

object AccessLogItems {
  def apply(cs: (String, Json)*): AccessLogItems = AccessLogItems(cs.toMap)
}

sealed trait FlowFailure
  case object RequestPluginFailure extends FlowFailure
  case object ResponsePluginFailure extends FlowFailure
  case object CallFailure extends FlowFailure

case class RequestCtx(
  request: TargetRequest,
  bodyStreamOpt: Option[ReadStream[Buffer]],
  original: OriginalRequest,
  properties: Properties = Properties(),
  tracingCtx: TracingContext,
  proxyHeaders: ProxyHeaders,
  authnCtx: AuthnCtx = AuthnCtx(),
  accessLog: AccessLogItems = AccessLogItems(),
  modifyResponse: List[ApiResponse => Future[ApiResponse]] = Nil,

  aborted: Option[ApiResponse] = None,
  failed: Option[FlowFailure] = None
) {
  def modifyRequest(f: TargetRequest => TargetRequest): RequestCtx =
    this.copy(request = f(request))

  def modifyProperties(f: Properties => Properties): RequestCtx =
    this.copy(properties = f(properties))

  def modifyAuthnCtx(f: AuthnCtx => AuthnCtx): RequestCtx =
    this.copy(authnCtx = f(authnCtx))

  def withAuthnCtx(name: String, value: Json): RequestCtx =
    this.copy(authnCtx = authnCtx.updated(name, value))

  def modifyAccessLog(f: AccessLogItems => AccessLogItems): RequestCtx =
    this.copy(accessLog = f(accessLog))

  def withAccessLog(name: String, value: Json): RequestCtx =
    this.copy(accessLog = accessLog.updated(name, value))

  def withTracingCtx(ctx: TracingContext): RequestCtx =
    this.copy(tracingCtx = ctx)

  def modifyResponse(response: ApiResponse)(implicit ec: ExecutionContext): Future[ApiResponse] =
    modifyResponse.foldLeft(Future.successful(response)) { case (fut, mod) => fut.flatMap(mod) }

  def withModifyResponse(f: ApiResponse => ApiResponse) =
    withModifyResponseAsync(f.andThen(Future.successful))

   def withModifyResponseAsync(f: ApiResponse => Future[ApiResponse]) =
    this.copy(modifyResponse = f :: modifyResponse)

  def abort(response: ApiResponse): RequestCtx =
    this.copy(aborted = Some(response))

  def isAborted(): Boolean =
    aborted.isDefined

  def isFailed(): Boolean =
    failed.isDefined
}

case class ResponseCtx(
  targetResponse: Option[ApiResponse],
  response: ApiResponse,
  request: TargetRequest,
  originalRequest: OriginalRequest,
  tracingCtx: TracingContext,
  properties: Properties = Properties(),
  authnCtx: AuthnCtx = AuthnCtx(),
  accessLog: AccessLogItems = AccessLogItems(),
  requestAborted: Boolean,
  failed: Option[FlowFailure] = None
) {
  def modifyResponse(f: ApiResponse => ApiResponse): ResponseCtx =
    this.copy(response = f(response))

  def withTracingCtx(ctx: TracingContext): ResponseCtx =
    this.copy(tracingCtx = ctx)

  def modifyProperties(f: Properties => Properties): ResponseCtx =
    this.copy(properties = f(properties))

  def modifyAccessLog(f: AccessLogItems => AccessLogItems): ResponseCtx =
    this.copy(accessLog = f(accessLog))

  def withAccessLog(name: String, value: Json): ResponseCtx =
    this.copy(accessLog = accessLog.updated(name, value))

  def isFailed(): Boolean =
    failed.isDefined
}

case class PluginName(value: String) extends AnyVal
case class PluginAddressPrefix(value: String) extends AnyVal
case class PluginConf(name: PluginName, conf: Json)
case class ApiGroupPluginConf(name: PluginName, conf: Json, addressPrefixOpt: Option[PluginAddressPrefix])

case class SmartHttpClientConf(value: JsonObject)
case class FixedHttpClientConf(value: JsonObject)

case class ProxyHeaders(headers: Map[String, List[String]], trueClientIp: String)
