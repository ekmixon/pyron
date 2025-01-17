package com.cloudentity.pyron

import com.cloudentity.pyron.api.{ApiHandlerVerticle, ApiServer}
import com.cloudentity.pyron.apigroup.ApiGroupsStoreVerticle
import com.cloudentity.pyron.config.Conf
import com.cloudentity.pyron.config.Conf.AppConf
import com.cloudentity.pyron.openapi.route.{GetOpenApiRoute, ListOpenApiRoute}
import com.cloudentity.pyron.rule.RulesStoreVerticle
import com.cloudentity.tools.vertx.bus.VertxEndpointClient
import com.cloudentity.tools.vertx.conf.ConfService
import com.cloudentity.tools.vertx.launchers.OrchisCommandLauncher
import com.cloudentity.tools.vertx.registry.RegistryVerticle
import com.cloudentity.tools.vertx.registry.RegistryVerticle.RegistryType
import com.cloudentity.tools.vertx.scala.{FutureConversions, ScalaSyntax, VertxExecutionContext}
import com.cloudentity.tools.vertx.server.VertxBootstrap
import com.cloudentity.tools.vertx.server.api.ApiServerDeployer
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.Verticle
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

class Application extends VertxBootstrap with FutureConversions with ScalaSyntax {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  implicit var ec: VertxExecutionContext = null

  var appConf: AppConf = _
  var confService: ConfService = _

  override def beforeServerStart(): VxFuture[_] = {
    ec = VertxExecutionContext(vertx.getOrCreateContext())
    confService = VertxEndpointClient.make(vertx, classOf[ConfService])

    for {
      appConf <- readAppConf
      _        = this.appConf = appConf
      _       <- deployRegistries()
      _       <- deployVerticle(new RulesStoreVerticle)
      _       <- deployVerticle(new ApiGroupsStoreVerticle)
      _       <- deployRegistryIfConfigured("open-api")
      _       <- deployRegistryIfConfigured("components")
    } yield ()
    }.toJava()

  def deployRegistries(): Future[Unit] =
    for {
      _ <- deployRegistryIfConfigured("sd")
      _ <- deployRegistryIfConfigured("system")
      _ <- deployRegistryIfConfigured("request-plugins")
      _ <- deployRegistryIfConfigured("response-plugins")
    } yield ()

  override def deployServer(): VxFuture[String] = {
    for {
      _          <- deployApiHandlers(appConf)
      _          <- deployServerInstance(appConf)
      _          <- deployAdminServerIfConfigured()
      _          <- deployOpenApiEndpointIfEnabled(appConf)
    } yield ""
  }.toJava

  private def readAppConf: Future[AppConf] = {
    for {
      confJsonOpt <- confService.getConf("app").toScala.map(Option.apply)
      confJson    <- confJsonOpt.map(Future.successful).getOrElse(Future.failed(new Exception("'app' configuration attribute missing")))
    } yield Conf.decodeUnsafe(confJson.toString)
  }

  def deployRegistryIfConfigured(registryType: String): Future[String] =
    VertxDeploy.deploy(vertx, new RegistryVerticle(new RegistryType(registryType), false)).toScala()

  // deploying ApiHandler per ApiServer
  private def deployApiHandlers(conf: AppConf): Future[Unit] =
    Future.sequence {
      (0 until getApiHandlerVerticlesNum(conf)).map { _ =>
        deployVerticle(new ApiHandlerVerticle())
      }
    }.map(_ => ())

  def deployServerInstance(conf: AppConf): Future[String] =
    deployVerticle(new ApiServer(conf))

  private def deployVerticle(verticle: Verticle): Future[String] = {
    log.debug(s"Deploying ${verticle.getClass.getName} verticle")
    VertxDeploy.deploy(vertx, verticle).toScala()
  }

  private def deployAdminServerIfConfigured(): Future[Unit] =
    confService.getConf("adminServer").toScala().map(Option.apply).flatMap {
      case Some(_) =>
        ApiServerDeployer.deployServer(vertx, "adminServer").map(()).toScala()
      case None    =>
        log.debug("Admin API server configuration missing, skipping deployment")
        Future.successful(())
    }

  private def deployOpenApiEndpointIfEnabled(conf: AppConf): Future[Unit] =
    if (conf.openApi.flatMap(_.enabled).getOrElse(true)) {
      deployVerticle(new GetOpenApiRoute()).map(_ => deployVerticle(new ListOpenApiRoute()))
    } else {
      log.info("OpenApi endpoint disabled, skipping deployment")
      Future.successful(())
    }

  def getApiHandlerVerticlesNum(conf: AppConf): Int =
    conf.serverVerticles.getOrElse(Runtime.getRuntime.availableProcessors)
}

object Application {
  def main(args: Array[String]): Unit = {
    OrchisCommandLauncher.main(args)
  }
}
