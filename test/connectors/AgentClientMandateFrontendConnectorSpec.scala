/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import builders.AuthBuilder
import models.{AgentEmail, ClientDisplayName}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.http.ws.WSHttp
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter

import scala.concurrent.Future

class AgentClientMandateFrontendConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
  implicit val request: Request[_] = FakeRequest(GET, "")
  val mockWSHttp = mock[WSHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestAgentClientMandateFrontendConnector extends AgentClientMandateFrontendConnector {
    override val http: HttpGet = mockWSHttp
    override def crypto: (String) => String = SessionCookieCryptoFilter.encrypt _
  }

  "AgentClientMandateFrontendConnector" must {
    "get agent email" in {
      val agentEmail = AgentEmail("aaa@bbb.com")
      when(mockWSHttp.GET[Option[AgentEmail]](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(agentEmail)))

      val response = await(TestAgentClientMandateFrontendConnector.getAgentEmail)
      response.get.email must be("aaa@bbb.com")
    }

    "get client display name" in {
      val displayName = ClientDisplayName("client display name")
      when(mockWSHttp.GET[Option[ClientDisplayName]](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(displayName)))

      val response = await(TestAgentClientMandateFrontendConnector.getClientDisplayName)
      response.get.name must be("client display name")
    }
  }
}
