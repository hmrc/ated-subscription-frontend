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

import java.util.UUID
import builders._
import models.{AtedSubscriptionAuthData, AtedUsers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtedConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  class Test extends ConnectorMocks {
    when(mockAppConfig.serviceUrlAtedSub).thenReturn("http://localhost:9020/")
    val testAtedConnector: AtedConnector = new AtedConnector(mockAppConfig, mockHttpClient) {
      override val serviceURL = "http://localhost:9020/test"
    }
  }
  override def beforeEach(): Unit = {
    reset(mockAppConfig)
  }

  "AtedConnector" must {

    "getDetails" must {
      "GET agent details from ETMP for a user" in new Test {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, "")))
        val result = testAtedConnector.getDetails("ARN1234567", "arn")
        await(result).status must be(OK)
        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "GET user details from ETMP for an agent" in new Test {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, "")))
        val result = testAtedConnector.getDetails("XN1200000100001", "arn")
        await(result).status must be(OK)
        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "GET subscription data from ETMP for an agent" in new Test {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, "")))
        val result = testAtedConnector.retrieveSubscriptionData("XN1200000100001")
        await(result).status must be(OK)
        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "GET user enrolments for a given safeID should return the list of group IDs" in new Test {
        val atedUsersList = AtedUsers(List("principalUserId1"), List("delegatedId1"))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val jsOnData = Json.toJson(atedUsersList)
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, jsOnData.toString())))

        val result = testAtedConnector.checkUsersEnrolments("XN1200000100001")
        await(result) must be(Some(atedUsersList))
        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "GET user enrolments for a given safeID should return the null if the call returns anything other than 200" in new Test {
        val atedUsersList = AtedUsers(List("principalUserId1"), List("delegatedId1"))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val jsOnData = Json.toJson(atedUsersList)
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(NOT_FOUND, jsOnData.toString())))
        val result = testAtedConnector.checkUsersEnrolments("XN1200000100001")
        await(result) must be(None)
        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "GET user enrolments for a given safeID should return internal server error if ETMP returns an error" in new Test {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(execute[HttpResponse]).thenReturn(Future.failed(new InternalServerException("ggghu")))
        val result = testAtedConnector.checkUsersEnrolments("XN1200000100001")
        the[InternalServerException] thrownBy await(result)
        verify(mockHttpClient, times(1)).get(any())(any())
      }
    }

  }
}
