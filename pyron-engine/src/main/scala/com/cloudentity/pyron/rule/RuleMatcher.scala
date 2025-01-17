package com.cloudentity.pyron.rule

import com.cloudentity.pyron.domain.flow.{BasePath, EndpointMatchCriteria, PathParams}
import io.vertx.core.http.HttpMethod

object RuleMatcher {
  sealed trait MatchResult
    case class Match(pathParams: PathParams) extends MatchResult
    case object NoMatch extends MatchResult

  def makeMatch(method: HttpMethod, path: String, basePath: BasePath, criteria: EndpointMatchCriteria): MatchResult =
    if (criteria.method != method) NoMatch
    else {
      val relativePath = path.drop(basePath.value.length)
      PathMatcher.makeMatch(relativePath, criteria.path)
        .fold[MatchResult](NoMatch)(Match)
    }
}
