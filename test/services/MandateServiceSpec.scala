/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import builders.AuthBuilder
import connectors.{AgentClientMandateConnector, AgentClientMandateFrontendConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class MandateServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "MandateService" must {

    "createMandateForNonUK" must {

      "return response, if mandate is created and status is CREATED" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAgentEmail)))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testClientDisplayName)))
        when(mockMandateConnector.createMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = TestMandateService.createMandateForNonUK("atedRefNum")
      }

      "return response, if mandate is created and status is CREATED but agent email and client display are are None" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateConnector.createMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = TestMandateService.createMandateForNonUK("atedRefNum")
      }

      "throw exception, if status is anything other than CREATED" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAgentEmail)))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testClientDisplayName)))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateConnector.createMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val thrown = the[RuntimeException] thrownBy await(TestMandateService.createMandateForNonUK("atedRefNum"))
        thrown.getMessage must be("Mandate creation failed.")
      }
    }

    "updateMandateForNonUK" must {

      "return response, if mandate is updated and status is CREATED" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAgentEmail)))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testClientDisplayName)))
        when(mockMandateConnector.updateMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = TestMandateService.updateMandateForNonUK("atedRefNum", "mandateId")
      }

      "return response, if mandate is created and status is CREATED but agent email and client display are are None" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateConnector.updateMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = TestMandateService.updateMandateForNonUK("atedRefNum", "mandateId")
      }

      "throw exception, if status is anything other than CREATED" in {
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAgentEmail)))
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testClientDisplayName)))
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockMandateConnector.updateMandateForNonUK(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val thrown = the[RuntimeException] thrownBy await(TestMandateService.updateMandateForNonUK("atedRefNum", "mandateId"))
        thrown.getMessage must be("Non-UK Mandate update failed.")
      }
    }

    "fetch email address from mandate for agent" must {
      "return the cached emailed address" in {
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAgentEmail)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = await(TestMandateService.fetchEmailAddress)
        result must be (Some(testAgentEmail))
      }
    }

    "don't fetch email address from mandate for client" must {
      "return the cached emailed address" in {
        when(mockMandateFrontendConnector.getAgentEmail(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = await(TestMandateService.fetchEmailAddress)
        result must be (None)
      }
    }

    "fetch client display name from mandate for agent" must {
      "return the cached client display name" in {
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testClientDisplayName)))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = await(TestMandateService.fetchClientDisplayName)
        result must be (Some(testClientDisplayName))
      }
    }

    "don't fetch client display name from mandate for client" must {
      "return the cached client display name" in {
        when(mockMandateFrontendConnector.getClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = await(TestMandateService.fetchClientDisplayName)
        result must be (None)
      }
    }

  }

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockMandateConnector = mock[AgentClientMandateConnector]
  val mockRegisteredBusinessService = mock[RegisteredBusinessService]
  val mockMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest(GET, "")

  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")
  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testReviewBusinessDetails = ReviewDetails(businessName = "test Name", businessType = None, businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)
  val testAgentEmail = AgentEmail("aa@mail.com")
  val testClientDisplayName = ClientDisplayName("client display name")

  object TestMandateService extends MandateService {
    override val dataCacheConnector = mockDataCacheConnector
    override val mandateConnector: AgentClientMandateConnector = mockMandateConnector
    override val mandateFrontendConnector = mockMandateFrontendConnector
    override val registeredBusinessService = mockRegisteredBusinessService
  }

}
