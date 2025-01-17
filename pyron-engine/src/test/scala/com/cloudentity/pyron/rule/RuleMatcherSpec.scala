package com.cloudentity.pyron.rule

import com.cloudentity.pyron.domain.flow.{BasePath, EndpointMatchCriteria, PathMatching, PathParams, PathPrefix}
import io.vertx.core.http.HttpMethod
import org.junit.runner.RunWith
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

@RunWith(classOf[JUnitRunner])
class RuleMatcherSpec extends WordSpec with MustMatchers with ScalaCheckDrivenPropertyChecks {

  import RuleMatcher._

  case class PathSuffix(value: String)

  implicit val pathPrefixArb: Arbitrary[PathPrefix] = Arbitrary(Gen.alphaNumStr.map(PathPrefix))
  implicit val pathSuffixArb: Arbitrary[PathSuffix] = Arbitrary(Gen.alphaNumStr.map(PathSuffix))
  implicit val methodArb: Arbitrary[HttpMethod] = Arbitrary(Gen.oneOf(HttpMethod.values()))

  "RuleMatcher.makeMatch" should {
    "return Match when basePath, path, prefix and method IS matching" in {
      forAll(minSuccessful(10)) { (method: HttpMethod, prefix: PathPrefix, suffix: PathSuffix) =>
        val criteria = EndpointMatchCriteria(method, PathMatching(s"^${prefix.value}${suffix.value}$$".r, Nil, prefix, ""))
        makeMatch(method, "/base-path" + prefix.value + suffix.value, BasePath("/base-path"), criteria) mustBe Match(PathParams(Map()))
      }
    }

    "return Match with path params" in {
      forAll(minSuccessful(10)) { (method: HttpMethod, prefix: PathPrefix, suffix: PathSuffix) =>
        val criteria = EndpointMatchCriteria(method, PathMatching(s"^${prefix.value}${suffix.value}$$".r, Nil, prefix, ""))
        makeMatch(method, prefix.value + suffix.value, BasePath(""), criteria) mustBe Match(PathParams(Map()))
      }
    }

    "return NoMatch when path with prefix IS NOT matching" in {
      val criteria = EndpointMatchCriteria(HttpMethod.GET, PathMatching("^/prefix/suffix$".r, Nil, PathPrefix("/prefix"), ""))
      makeMatch(HttpMethod.GET, "/suffix", BasePath(""), criteria) mustBe NoMatch
    }

    "return NoMatch when base-path IS NOT matching" in {
      val criteria = EndpointMatchCriteria(HttpMethod.GET, PathMatching("^/prefix/suffix$".r, Nil, PathPrefix("/prefix"), ""))
      makeMatch(HttpMethod.GET, "/other-base-path/prefix/suffix", BasePath("/base-path"), criteria) mustBe NoMatch
    }

    "return NoMatch when path wo prefix IS NOT matching" in {
      val criteria = EndpointMatchCriteria(HttpMethod.GET, PathMatching("^/suffix$".r, Nil, PathPrefix(""), ""))
      makeMatch(HttpMethod.GET, "/prefix/suffix", BasePath(""), criteria) mustBe NoMatch
    }

    "return NoMatch when path with prefix IS matching and method IS NOT matching" in {
      val criteria = EndpointMatchCriteria(HttpMethod.GET, PathMatching("^/prefix/suffix$".r, Nil, PathPrefix("/prefix"), ""))
      makeMatch(HttpMethod.POST, "/prefix/suffix", BasePath(""), criteria) mustBe NoMatch
    }

    "return NoMatch when path with prefix IS NOT matching and method IS matching" in {
      val criteria = EndpointMatchCriteria(HttpMethod.GET, PathMatching("^/prefix/suffix$".r, Nil, PathPrefix("/prefix"), ""))
      makeMatch(HttpMethod.GET, "/suffix", BasePath(""), criteria) mustBe NoMatch
    }
  }
}
