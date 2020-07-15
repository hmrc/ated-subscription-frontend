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

package services

import builders.AuthBuilder
import connectors.{AgentClientMandateFrontendConnector, AtedConnector, BusinessCustomerFrontendConnector}
import models._
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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegisteredBusinessServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockBusinessCustomerFrontendConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockAgentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]

  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testReviewBusinessDetails = BusinessCustomerDetails(businessName = "test Name", businessType = "Corporate Body", businessAddress = testAddress,
    sapNumber =  "1234567890", safeId =  "EX0012345678909", agentReferenceNumber =  None)

  val testRegisteredBusinessServices = new RegisteredBusinessService(mockBusinessCustomerFrontendConnector, mockAtedConnector, mockAgentClientMandateFrontendConnector)

  override def beforeEach(): Unit = {
    reset(mockBusinessCustomerFrontendConnector)
    reset(mockAtedConnector)
    reset(mockAgentClientMandateFrontendConnector)
  }

  "RegisteredBusinessService" must {

    "return review business details, if review details found from Keystore" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = testRegisteredBusinessServices.getBusinessCustomerDetails
      await(result) must be(testReviewBusinessDetails)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return review business details, if review details NOT found from Keystore but there are old mandate details" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      val addressDetails = AddressDetails(addressType = "Permanent Place Of Business", addressLine1 = "", addressLine2 = "", countryCode = "GB")
      val subscriptionData = SubscriptionData("safeId", "orgName", emailConsent = None, address = Seq(SubscriptionAddress(Some("name1"), Some("name2"), addressDetails = addressDetails)))
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(oldMandateRef)))
      when(mockAtedConnector.retrieveSubscriptionData(ArgumentMatchers.eq("atedRefNo"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(subscriptionData)))))

      val result = testRegisteredBusinessServices.getBusinessCustomerDetails
      await(result).businessName must be(subscriptionData.organisationName)
    }

    "throw an exception, if review details NOT found from Keystore for old mandate details but no subscription data found " in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      val oldMandateRef = OldMandateReference("mandateId", "atedRefNo")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(oldMandateRef)))
      when(mockAtedConnector.retrieveSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(testRegisteredBusinessServices.getBusinessCustomerDetails)
      thrown.getMessage must include("Error while retrieving subscription data for ated ref no: atedRefNo  status:: 500")
    }

    "throw an exception, if review details from keystore gives back any other status" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(testRegisteredBusinessServices.getBusinessCustomerDetails)
      thrown.getMessage must include("Error while retrieving review details from business-customer keystore")
    }

    "throw an exception, if review details NOT found and no old mandate details found " in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockAgentClientMandateFrontendConnector.getOldMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockAtedConnector.retrieveSubscriptionData(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
      val thrown = the[RuntimeException] thrownBy await(testRegisteredBusinessServices.getBusinessCustomerDetails)
      thrown.getMessage must include("No Old Mandate Reference found for the client")
    }

    "return Business Address, if review details found from Keystore" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = testRegisteredBusinessServices.getBusinessAddress
      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return Correspondence Address if we are a normal user" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = testRegisteredBusinessServices.getDefaultCorrespondenceAddress()

      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "return Correspondence Address if we are an agent, but no address found" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockAtedConnector.getDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockBusinessCustomerFrontendConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(testReviewBusinessDetails)))))
      val result = testRegisteredBusinessServices.getDefaultCorrespondenceAddress()

      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
      verify(mockAtedConnector, times(1)).getDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
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
      when(mockAtedConnector.getDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val result = testRegisteredBusinessServices.getDefaultCorrespondenceAddress()

      await(result).toString must be("Line 1, Line 2, Line 3, Line 4, AA1 1AA, GB")
      verify(mockAtedConnector, times(1)).getDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

  }

}
