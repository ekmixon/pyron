package com.cloudentity.edge.plugin

import com.cloudentity.edge.plugin.bus.request._
import com.cloudentity.tools.vertx.bus.VertxEndpoint
import com.cloudentity.tools.vertx.tracing.TracingContext
import io.vertx.core.Future

trait RequestPluginService extends ValidatePluginService with ConvertOpenApiService {
  @VertxEndpoint(address = ":request-plugin.apply")
  def applyPlugin(ctx: TracingContext, req: ApplyRequest): Future[ApplyResponse]
}
