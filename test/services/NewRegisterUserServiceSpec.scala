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
import connectors._
import models._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class NewRegisterUserServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest(GET, "")

  val subscribeSuccessResponse = SubscribeSuccessResponse(processingDate = Some("2001-12-17T09:30:47Z"),
    atedRefNumber = Some("ABCDEabcde12345"), formBundleNumber = Some("123456789012345"))
  val enrolSuccessResponse = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))
  val testAddress = Address("line_1", "line_2", None, None, postcode = Some("XX11XX"), "GB")
  val testAddressNoPOstCode = Address("line_1", "line_2", None, None, postcode = None, "GB")
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")

  val testReviewBusinessDetails = ReviewDetails(businessName = "test Name", utr = Some("1111111111"), businessType = Some("test Type"), businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsforSOP = ReviewDetails(businessName = "test Name", utr = Some("1111111111"), businessType = Some("SOP"), businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoPostCode = ReviewDetails(businessName = "test Name", utr = Some("1111111111"), businessType = Some("SOP"), businessAddress = testAddressNoPOstCode,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoUtrPostCode = ReviewDetails(businessName = "test Name", businessType = Some("SOP"), businessAddress = testAddressNoPOstCode,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoUtr = ReviewDetails(businessName = "test Name", utr = None, businessType = Some("SOP"), businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val mockAtedSubscriptionConnector = mock[AtedSubscriptionConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockTaxEnrolmentConnector = mock[TaxEnrolmentsConnector]
  val mockRegisteredBusinessService = mock[RegisteredBusinessService]
  val mockAuthClientConnector = mock[AuthConnector]

  override def beforeEach() = {
    reset(mockAtedSubscriptionConnector)
    reset(mockDataCacheConnector)
    reset(mockRegisteredBusinessService)
    reset(mockAuthClientConnector)
  }

  object TestNewRegisterUserService extends NewRegisterUserService {
    val atedSubscriptionConnector: AtedSubscriptionConnector = mockAtedSubscriptionConnector
    val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    val registeredBusinessService: RegisteredBusinessService = mockRegisteredBusinessService
    val authConnector: AuthConnector = mockAuthClientConnector
    val taxEnrolmentsConnector: TaxEnrolmentsConnector = mockTaxEnrolmentConnector
  }

  "NewRegisterUserService" must {

    "use correct connectors" in {
      RegisterUserService.atedSubscriptionConnector must be(AtedSubscriptionConnector)
      RegisterUserService.dataCacheConnector must be(AtedSubscriptionDataCacheConnector)
      RegisterUserService.governmentGatewayConnector must be(GovernmentGatewayConnector)
    }

  }

    "subscribeAted" must {
      "if successful, should return subscribe success response for company user" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = await(TestNewRegisterUserService.subscribeAted())
        result._1 must be(subscribeSuccessResponse)
      }
      "if successful, should return subscribe success response for sole user" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetailsforSOP))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = await(TestNewRegisterUserService.subscribeAted())
        result._1 must be(subscribeSuccessResponse)
      }

      "if successful, should return subscribe success response for Non-UK Clients" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetailsNoUtr))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = await(TestNewRegisterUserService.subscribeAted())
        result._1 must be(subscribeSuccessResponse)
      }

      "when called by agent to register non-uk client, if successful, should return subscribe success response and not enrol current credential" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
        val result = await(TestNewRegisterUserService.subscribeAted(true))
        result._2.json must be(enrolSuccessResponse)
      }

      "throw exception for invalid users" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), None)))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("Failed to enrol - user did not have a group identifier (not a valid GG user)")
      }

      "throw exception when utr and postcode not present" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetailsNoUtrPostCode))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("[NewRegisterUserService][subscribeAted][createEMACEnrolRequest] - postalCode or utr must be supplied")
      }

      "throw exception when postcode not present" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetailsNoPostCode))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddressNoPOstCode)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("[NewRegisterUserService][subscribeAted][createEMACEnrolRequest] - postalCode must be supplied")
      }

      "should handle invalid data in the subscribe success response" in {
        val invalidSuccessResponse = SubscribeSuccessResponse(None, None, None)

        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(invalidSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("ated reference number not returned" )
      }

      "if unsuccessful, should throw runtime exception - cause address is not in keystore" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("data not found")
      }
      "if unsuccessful, should throw runtime exception - cause contact details is not in keystore" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("data not found")
      }
      "if unsuccessful, should throw runtime exception - cause contact details email is not in keystore" in {
        when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
        when(mockDataCacheConnector.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        when(mockDataCacheConnector.fetchContactDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession(Matchers.any())).thenReturn(Future.successful(None))
        when(mockAtedSubscriptionConnector.subscribeAted(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(subscribeSuccessResponse))
        when(mockTaxEnrolmentConnector.enrol(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        val result = TestNewRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("data not found")
      }
    }


  "toEtmpAddress" must {
    "correctly populate the address" in {
      val testAddress = Address("line_1", "line_2", None, None, None, "GB")
      val etmpAddress = TestNewRegisterUserService.toEtmpAddress(testAddress)
      etmpAddress.addressLine1 must be ("line_1")
      etmpAddress.addressLine2 must be ("line_2")
      etmpAddress.addressLine3.isDefined must be (false)
      etmpAddress.addressLine4.isDefined must be (false)
      etmpAddress.postalCode.isDefined must be (false)
      etmpAddress.countryCode must be ("GB")
    }

    "correctly populate the address when we have all data including an unformatted postcode" in {
      val testAddress = Address("line_1", "line_2", Some("line_3"), Some("line_4"), Some("ne12je"), "GB")
      val etmpAddress = TestNewRegisterUserService.toEtmpAddress(testAddress)
      etmpAddress.addressLine1 must be ("line_1")
      etmpAddress.addressLine2 must be ("line_2")
      etmpAddress.addressLine3 must be (Some("line_3"))
      etmpAddress.addressLine4 must be (Some("line_4"))
      etmpAddress.postalCode must be (Some("NE1 2JE"))
      etmpAddress.countryCode must be ("GB")
    }

    "correctly setup EtmpContactDetails if we have no email or phone number" in {
      val testContact = ContactDetails("ABC", "DEF", "")
      val testContactEmail = ContactDetailsEmail(Some(false), "")
      val etmpContactDetails = TestNewRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
      etmpContactDetails.emailAddress must be (None)
      etmpContactDetails.faxNumber must be (None)
      etmpContactDetails.mobileNumber must be (None)
      etmpContactDetails.phoneNumber must be (None)
    }
  }

  "toEtmpContactDetails" must {
    "correctly setup EtmpContactDetails if we have an email" in {
      val testContact = ContactDetails("ABC", "DEF", "1234567890")
      val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")
      val etmpContactDetails = TestNewRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
      etmpContactDetails.emailAddress must be (Some("abc@test.com"))
      etmpContactDetails.faxNumber must be (None)
      etmpContactDetails.mobileNumber must be (None)
      etmpContactDetails.phoneNumber must be (Some("1234567890"))
    }

    "correctly setup EtmpContactDetails if we have no email or phone number" in {
      val testContact = ContactDetails("ABC", "DEF", "")
      val testContactEmail = ContactDetailsEmail(Some(false), "")
      val etmpContactDetails = TestNewRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
      etmpContactDetails.emailAddress must be (None)
      etmpContactDetails.faxNumber must be (None)
      etmpContactDetails.mobileNumber must be (None)
      etmpContactDetails.phoneNumber must be (None)
    }
  }
}
