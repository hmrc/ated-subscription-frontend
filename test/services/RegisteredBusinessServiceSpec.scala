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

package services

import builders.AuthBuilder
import connectors.{AgentClientMandateFrontendConnector, AtedConnector, BusinessCustomerFrontendConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class RegisteredBusinessServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockBusinessCustomerFrontendConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]

  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testReviewBusinessDetails = ReviewDetails(businessName = "test Name", businessType = None, businessAddress = testAddress,
    sapNumber =  "1234567890", safeId =  "EX0012345678909", agentReferenceNumber =  None)

  object TestRegisteredBusinessServices extends RegisteredBusinessService {
    override val businessCustomerFrontendConnector = mockBusinessCustomerFrontendConnector
    override val atedConnector = mockAtedConnector
    override val agentClientMandateFrontendConnector = mockAgentClientMandateFrontendConnector
  }

  override def beforeEach(): Unit = {
    reset(mockBusinessCustomerFrontendConnector)
    reset(mockAtedConnector)
    reset(mockAgentClientMandateFrontendConnector)
  }

  "RegisteredBusinessServices" must {

    "return review business details, if review details found from Keystore" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = TestRegisteredBusinessServices.getReviewBusinessDetails
      await(result) must be(testReviewBusinessDetails)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
    }

    "return review business details, if review details NOT found from Keystore but there are old mandate details" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      val addressDetails = AddressDetails(addressType = "Permanent Place Of Business", addressLine1 = "", addressLine2 = "", countryCode = "GB")
      val subscriptionData = SubscriptionData("safeId", "orgName", emailConsent = None, address = Seq(SubscriptionAddress(Some("name1"), Some("name2"), addressDetails = addressDetails)))
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(oldMandateRef)))
      when(mockAtedConnector.retrieveSubscriptionData(Matchers.eq("atedRefNo"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(subscriptionData)))))

      val result = TestRegisteredBusinessServices.getReviewBusinessDetails
      await(result).businessName must be(subscriptionData.organisationName)
    }

    "throw an exception, if review details NOT found from Keystore for old mandate details but no subscription data found " in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(oldMandateRef)))
      when(mockAtedConnector.retrieveSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(TestRegisteredBusinessServices.getReviewBusinessDetails)
      thrown.getMessage must include("Error while retrieving subscription data for ated ref no: atedRefNo  status:: 500")
    }

    "throw an exception, if review details from keystore gives back any other status" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(TestRegisteredBusinessServices.getReviewBusinessDetails)
      thrown.getMessage must include("Error while retrieving review details from business-customer keystore")
    }

    "throw an exception, if review details NOT found and no old mandate details found " in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockAtedConnector.retrieveSubscriptionData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(TestRegisteredBusinessServices.getReviewBusinessDetails)
      thrown.getMessage must include("No Old Mandate Reference found for the client")
    }



    "return Business Address, if review details found from Keystore" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = TestRegisteredBusinessServices.getBusinessAddress
      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
    }

    "return Correspondence Address if we are a normal user" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = TestRegisteredBusinessServices.getDefaultCorrespondenceAddress

      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
    }

    "return Correspondence Address if we are an agent, but no address found" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockAtedConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = TestRegisteredBusinessServices.getDefaultCorrespondenceAddress

      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
      verify(mockAtedConnector, times(1)).getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
    }

    "return Correspondence Address from the agent if we are one" in {
      val successResponse = Json.parse(
        """
          |{
          |  "sapNumber":"1234567890", "safeId": "EX0012345678909",
          |  "agentReferenceNumber": "AARN1234567",
          |  "isAnIndividual": true,
          |  "isAnAgent": true,
          |  "isEditable": true,
          |  "individual": {
          |    "firstName": "abc",
          |    "lastName": "xyz",
          |    "dateOfBirth": "1962-10-12"
          |  },
          |  "addressDetails": {
          |    "addressLine1": "Line 1",
          |    "addressLine2": "Line 2",
          |    "addressLine3": "Line 3",
          |    "addressLine4": "Line 4",
          |    "postalCode": "AA1 1AA",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails" : {}
          |}
        """.stripMargin
      )

      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockAtedConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val result = TestRegisteredBusinessServices.getDefaultCorrespondenceAddress

      await(result).toString must be("Line 1, Line 2, Line 3, Line 4, AA1 1AA, GB")
      verify(mockAtedConnector, times(1)).getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
    }

  }

}
