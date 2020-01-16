package helpers

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.application.IntegrationApplication
import helpers.wiremock.WireMockSetup
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSRequest

trait IntegrationSpec extends PlaySpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with IntegrationApplication
  with WireMockSetup
  with AssertionHelpers
  with LoginStub {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def stubPost(url: String, status: Integer, responseBody: String) =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubGet(url: String, status: Integer, body: String) =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def hitApplicationEndpoint(url: String): WSRequest = {
    val appendSlash = if(url.startsWith("/")) url else s"/$url"
    ws.url(s"$testAppUrl$appendSlash")
      .withFollowRedirects(false)
  }
}
