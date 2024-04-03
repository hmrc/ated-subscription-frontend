/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
