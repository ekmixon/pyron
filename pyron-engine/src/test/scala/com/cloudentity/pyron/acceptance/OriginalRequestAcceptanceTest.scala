package com.cloudentity.pyron.acceptance

import com.cloudentity.pyron.PyronAcceptanceTest
import com.cloudentity.pyron.domain.Codecs._
import com.cloudentity.pyron.domain.flow.PathParams
import com.cloudentity.pyron.domain.http.{FixedRelativeUri, OriginalRequest, QueryParams, UriPath}
import com.cloudentity.pyron.util.MockUtils
import com.cloudentity.tools.vertx.http.Headers
import io.circe.parser._
import io.circe.syntax._
import io.restassured.RestAssured.given
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import org.hamcrest.{BaseMatcher, Description}
import org.junit.{After, Before, Test}
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.scalatest.MustMatchers
import org.slf4j.{Logger, LoggerFactory}

class OriginalRequestAcceptanceTest extends PyronAcceptanceTest with MustMatchers with MockUtils {
  var targetService: ClientAndServer = _

  @Before
  def before(): Unit = {
    targetService = startClientAndServer(7760)
  }

  @After
  def finish(): Unit = {
    targetService.stop
  }

  override def getMetaConfPath: String = "src/test/resources/acceptance/original/meta-config.json"

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  @Test
  def shouldSetTargetRequestOriginalPathParamsAndQueryParamsAndMethod(): Unit = {
    val relativeUri = FixedRelativeUri(UriPath("/service/params/value1"), QueryParams(Map("queryParam1" -> List("queryValue1"))), PathParams(Map()))
    val original = OriginalRequest(HttpMethod.GET, UriPath(relativeUri.path), QueryParams("queryParam1" -> List("queryValue1")), Headers(), Some(Buffer.buffer()), PathParams(Map("param1" -> "value1")))

      given()
      .when()
        .get(relativeUri.value)
      .`then`()
        .body(bodyMatcher(original))
    }

  @Test
  def shouldSetTargetRequestOriginalBody(): Unit = {
      val path = "/service/body"
      val body = """test-body"""
      val original = OriginalRequest(HttpMethod.POST, UriPath(path), QueryParams.empty, Headers(), Some(Buffer.buffer(body)), PathParams.empty)

      given().body(body)
      .when().post(path)
      .`then`()
        .body(bodyMatcher(original))
    }

  @Test
  def shouldSetTargetRequestOriginalHeaders(): Unit = {
      val path = "/service/headers"

      given()
        .header("header1", "value1")
      .when()
        .get(path)
      .`then`()
        .body(headersInBodyMatcher(Map("header1" -> List("value1"))))
    }

  def bodyMatcher(expected: OriginalRequest): BaseMatcher[_] =
    new BaseMatcher {
      override def matches(o: Any): Boolean = {
        val actual = decode[OriginalRequest](o.toString).getOrElse(throw new Exception)
        actual.copy(headers = Headers()) == expected // checking only one header to avoid checking headers set by RestAssured
      }

      override def describeTo(description: Description): Unit =
        description.appendText(expected.asJson.noSpaces)
    }

  def headersInBodyMatcher(expected: Map[String, List[String]]): BaseMatcher[_] =
    new BaseMatcher {
      override def matches(o: Any): Boolean = {
        val actual = decode[OriginalRequest](o.toString).getOrElse(throw new Exception)
        expected.forall { case (key, value) =>
          actual.headers.exists(_ == (key, value))
        }
      }

      override def describeTo(description: Description): Unit =
        description.appendText(expected.asJson.noSpaces)
    }
}
