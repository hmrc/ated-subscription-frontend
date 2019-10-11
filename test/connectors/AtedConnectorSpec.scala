/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import builders._
import models.AtedSubscriptionAuthData
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtedConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val testAtedConnector: AtedConnector = new AtedConnector(mockAppConfig, mockWSHttp) {
    override val serviceURL = "test"
  }

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
  }

  "AtedConnector" must {

    "getDetails" must {
      "GET agent details from ETMP for a user" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = testAtedConnector.getDetails("ARN1234567", "arn")
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "GET user details from ETMP for an agent" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = testAtedConnector.getDetails("XN1200000100001", "arn")
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "GET subscription data from ETMP for an agent" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = testAtedConnector.retrieveSubscriptionData("XN1200000100001")
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

  }
}
