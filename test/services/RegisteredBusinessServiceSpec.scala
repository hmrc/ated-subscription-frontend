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

package services

import builders.AuthBuilder
import connectors.{BusinessCustomerFrontendConnector, AtedConnector}
import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}

import scala.concurrent.Future

class RegisteredBusinessServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockBusinessCustomerFrontendConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]

  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testReviewBusinessDetails = ReviewDetails(businessName = "test Name", businessType = None, businessAddress = testAddress,
    sapNumber =  "1234567890", safeId =  "EX0012345678909", agentReferenceNumber =  None)



  object TestRegisteredBusinessServices extends RegisteredBusinessService {
    override val businessCustomerFrontendConnector = mockBusinessCustomerFrontendConnector
    override val atedConnector = mockAtedConnector
  }

  override def beforeEach(): Unit = {
    reset(mockBusinessCustomerFrontendConnector)
    reset(mockAtedConnector)
  }

  "RegisteredBusinessServices" must {

    "return Business Address, if review details found from Keystore" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
      val result = TestRegisteredBusinessServices.getBusinessAddress
      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
    }

    "return Correspondence Address if we are a normal user" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
      val result = TestRegisteredBusinessServices.getDefaultCorrespondenceAddress

      await(result).toString must be(testReviewBusinessDetails.businessAddress.toString)
      verify(mockBusinessCustomerFrontendConnector, times(1)).getReviewDetails(Matchers.any(), Matchers.any())
    }

    "return Correspondence Address if we are an agent, but no address found" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      implicit val request: Request[_] = FakeRequest(GET, "")
      when(mockAtedConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, None)))
      when(mockBusinessCustomerFrontendConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
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
