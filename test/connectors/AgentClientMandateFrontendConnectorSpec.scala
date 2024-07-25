/*
 * Copyright 2023 HM Revenue & Customs
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


  class Test extends ConnectorMocks {
    val testAgentClientMandateConnector: AgentClientMandateConnector = new AgentClientMandateConnector(mockAppConfig, mockHttpClient) {
      override lazy val serviceURL: String = "http://localhost:9020/test"
    }

    when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)
    val testAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = new AgentClientMandateFrontendConnector(
      mockAppConfig,
      mockHttpClient
    ){
      override val serviceUrl: String = "http://localhost:9020/test"
    }
  }

  "AgentClientMandateFrontendConnector" must {
    "get agent email" in new Test {
      val agentEmail = AgentEmail("aaa@bbb.com")
      when(execute[Option[AgentEmail]]).thenReturn(Future.successful(Some(agentEmail)))
      val response = await(testAgentClientMandateFrontendConnector.getAgentEmail)
      response.get.email must be("aaa@bbb.com")
    }

    "get client display name" in new Test {
      val displayName = ClientDisplayName("client display name")
      when(execute[Option[ClientDisplayName]]).thenReturn(Future.successful(Some(displayName)))

      val response = await(testAgentClientMandateFrontendConnector.getClientDisplayName)
      response.get.name must be("client display name")
    }

    "get old mandate details" in new Test {
      val oldMandateDetails = OldMandateReference("mandateId", "atedRef")
      when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, Json.toJson(oldMandateDetails).toString)))

      val response = await(testAgentClientMandateFrontendConnector.getOldMandateDetails)
      response.get.atedRefNumber must be("atedRef")
    }
  }
}
