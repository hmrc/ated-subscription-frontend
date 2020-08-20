/*
 * Copyright 2020 HM Revenue & Customs
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
import models.{AgentEmail, AtedSubscriptionAuthData, ClientDisplayName, OldMandateReference}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientMandateFrontendConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
  implicit val request: Request[_] = FakeRequest(GET, "")

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
  }

  when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)

  val testAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = new AgentClientMandateFrontendConnector(
    mockAppConfig,
    mockWSHttp
  ){
    override val serviceUrl: String = "test"
  }

  "AgentClientMandateFrontendConnector" must {
    "get agent email" in {
      val agentEmail = AgentEmail("aaa@bbb.com")
      when(mockWSHttp.GET[Option[AgentEmail]](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(agentEmail)))

      val response = await(testAgentClientMandateFrontendConnector.getAgentEmail)
      response.get.email must be("aaa@bbb.com")
    }

    "get client display name" in {
      val displayName = ClientDisplayName("client display name")
      when(mockWSHttp.GET[Option[ClientDisplayName]](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(displayName)))

      val response = await(testAgentClientMandateFrontendConnector.getClientDisplayName)
      response.get.name must be("client display name")
    }

    "get old mandate details" in {
      val oldMandateDetails = OldMandateReference("mandateId", "atedRef")
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse.apply(OK, Json.toJson(oldMandateDetails).toString)))

      val response = await(testAgentClientMandateFrontendConnector.getOldMandateDetails)
      response.get.atedRefNumber must be("atedRef")
    }
  }
}
