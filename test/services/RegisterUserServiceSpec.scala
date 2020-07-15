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
import connectors._
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegisterUserServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest(GET, "")
  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

  val subscribeSuccessResponse = SubscribeSuccessResponse(processingDate = Some("2001-12-17T09:30:47Z"),
    atedRefNumber = Some("ABCDEabcde12345"), formBundleNumber = Some("123456789012345"))
  val enrolSuccessResponse: JsValue = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))
  val testAddress = Address("line_1", "line_2", None, None, postcode = Some("XX11XX"), "GB")
  val testAddressNoPOstCode = Address("line_1", "line_2", None, None, postcode = None, "GB")
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")

  val testReviewBusinessDetails = BusinessCustomerDetails(businessName = "test Name", utr = Some("1111111111"),
    businessType = "Corporate Body", businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsforPartnership = BusinessCustomerDetails(businessName = "test Name", utr = Some("1111111111"),
    businessType = "Partnership", businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoPostCode = BusinessCustomerDetails(businessName = "test Name", utr = Some("1111111111"),
    businessType = "Non UK-based Company", businessAddress = testAddressNoPOstCode,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoUtrPostCode = BusinessCustomerDetails(businessName = "test Name",
    businessType = "LLP", businessAddress = testAddressNoPOstCode,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testReviewBusinessDetailsNoUtr = BusinessCustomerDetails(businessName = "test Name", utr = None,
    businessType = "Non UK-based Company", businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val mockAtedSubscriptionConnector: AtedSubscriptionConnector = mock[AtedSubscriptionConnector]
  val mockTaxEnrolmentConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]

  def subscribeMocks(businessCustomerDetails: BusinessCustomerDetails = testReviewBusinessDetails,
                     correspondenceAddress: Option[Address] = Some(testAddress),
                     contactDetails: Option[ContactDetails] = Some(testContact),
                     contactEmail: Option[ContactDetailsEmail] = Some(testContactEmail),
                     subscribeAtedResponse: Future[SubscribeSuccessResponse] = Future.successful(subscribeSuccessResponse)) = {
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(businessCustomerDetails))
    when(mockDataCacheConnector.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(correspondenceAddress))
    when(mockDataCacheConnector.fetchContactDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(contactDetails))
    when(mockDataCacheConnector.fetchContactDetailsEmailForSession(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(contactEmail))
    when(mockAtedSubscriptionConnector.subscribeAted(
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(subscribeAtedResponse)
  }

  def enrolMocks(businessCustomerDetails: BusinessCustomerDetails = testReviewBusinessDetails,
                 invalidUser: Boolean = false): OngoingStubbing[Future[HttpResponse]] = {
    if(invalidUser){
      when(mockAuthConnector.authorise[Any](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), None)))
    }else{
      when(mockAuthConnector.authorise[Any](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~ (Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
    }
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(businessCustomerDetails))
    when(mockTaxEnrolmentConnector.enrol(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(HttpResponse(CREATED, Some(enrolSuccessResponse))))
  }

  override def beforeEach(): Unit = {
    reset(mockAtedSubscriptionConnector)
    reset(mockDataCacheConnector)
    reset(mockRegisteredBusinessService)
    reset(mockAuthConnector)
  }

  val testRegisterUserService = new RegisterUserService(mockAppConfig, mockAtedSubscriptionConnector,
    mockDataCacheConnector, mockRegisteredBusinessService, mockTaxEnrolmentConnector, mockAuthConnector)

    "subscribeAted" must {
      "if successful, return subscribe success response for company user" in {
        subscribeMocks()
        val result = await(testRegisterUserService.subscribeAted())
        result must be(subscribeSuccessResponse)
      }

      "if successful, should return subscribe success response for Partnership" in {
        subscribeMocks(businessCustomerDetails = testReviewBusinessDetailsforPartnership)
        val result = await(testRegisterUserService.subscribeAted())
        result must be(subscribeSuccessResponse)
      }

      "if successful, should return subscribe success response for Non-UK Clients" in {
        subscribeMocks(businessCustomerDetails = testReviewBusinessDetailsNoUtr)
        val result = await(testRegisterUserService.subscribeAted())
        result must be(subscribeSuccessResponse)
      }

      "when called by agent to register non-uk client, if successful, should return subscribe success response and not enrol current credential" in {
        subscribeMocks()
        val result = await(testRegisterUserService.subscribeAted(true))
        result must be(subscribeSuccessResponse)
      }

      "not throw an exception when postcode not present but utr is" in {
        subscribeMocks(correspondenceAddress = Some(testAddressNoPOstCode))
        val result = await(testRegisterUserService.subscribeAted())
        result must be(subscribeSuccessResponse)
      }

      "throw a runtime exception if no address is found in keystore" in {
        subscribeMocks(correspondenceAddress = None)
        val result = testRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("address not found")
      }

      "throw runtime exception if no contact details are found in keystore" in {
        subscribeMocks(contactDetails = None)
        val result = testRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("contact details not found")
      }

      "throw a runtime exception if contact details email is not found in keystore" in {
        subscribeMocks(contactEmail = None)
        val result = testRegisterUserService.subscribeAted()
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("contact email not found")
      }
    }

  "enrolAted" must {

    "be successful for a Corporate Body entity type" in {
      enrolMocks(businessCustomerDetails = testReviewBusinessDetails)
      val result = await(testRegisterUserService.enrolAted(subscribeSuccessResponse))
      result.status must be(CREATED)
    }

    "be successful for a non-Corporate Body entity type" in {
      enrolMocks(businessCustomerDetails = testReviewBusinessDetailsforPartnership)
      val result = await(testRegisterUserService.enrolAted(subscribeSuccessResponse))
      result.status must be(CREATED)
    }

    "be successful when there is no utr but there is a postcode" in {
      enrolMocks(businessCustomerDetails = testReviewBusinessDetailsNoUtr)
      val result = await(testRegisterUserService.enrolAted(subscribeSuccessResponse))
      result.status must be(CREATED)
    }

    "be successful when there is a utr but there is no postcode" in {
      enrolMocks(businessCustomerDetails = testReviewBusinessDetailsNoPostCode)
      val result = await(testRegisterUserService.enrolAted(subscribeSuccessResponse))
      result.status must be(CREATED)
    }

    "throw exception when utr and postcode not present" in {
      enrolMocks(businessCustomerDetails = testReviewBusinessDetailsNoUtrPostCode)
      val result = testRegisterUserService.enrolAted(subscribeSuccessResponse)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("[RegisterUserService][createEnrolmentVerifiers] - postcode or utr must be supplied")
    }

    "throw exception for invalid users" in {
      enrolMocks(invalidUser = true)
      val result = testRegisterUserService.enrolAted(subscribeSuccessResponse)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Failed to enrol - user did not have a group identifier (not a valid GG user)")
    }

    "should throw an exception if there is invalid data in the subscribe success response" in {
      val invalidSuccessResponse = SubscribeSuccessResponse(None, None, None)
      enrolMocks()
      val result = testRegisterUserService.enrolAted(invalidSuccessResponse)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("ated reference number not returned" )
    }

    "not enrol if the the user is an agent registering a non-UK client" in {
      val result = await(testRegisterUserService.enrolAted(subscribeSuccessResponse, isNonUKClientRegisteredByAgent = true))
      result.status must be(OK)
      result.json must be(Json.parse("""{ "serviceName" : "ated","state" : "NotEnroled","identifiers" : [ ]}"""))
    }

  }

  "toEtmpAddress" must {
    "correctly populate the address" in {
      val testAddress = Address("line_1", "line_2", None, None, None, "GB")
      val etmpAddress = testRegisterUserService.toEtmpAddress(testAddress)
      etmpAddress.addressLine1 must be ("line_1")
      etmpAddress.addressLine2 must be ("line_2")
      etmpAddress.addressLine3.isDefined must be (false)
      etmpAddress.addressLine4.isDefined must be (false)
      etmpAddress.postalCode.isDefined must be (false)
      etmpAddress.countryCode must be ("GB")
    }

    "correctly populate the address when we have all data including an unformatted postcode" in {
      val testAddress = Address("line_1", "line_2", Some("line_3"), Some("line_4"), Some("ne12je"), "GB")
      val etmpAddress = testRegisterUserService.toEtmpAddress(testAddress)
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
      val etmpContactDetails = testRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
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
      val etmpContactDetails = testRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
      etmpContactDetails.emailAddress must be (Some("abc@test.com"))
      etmpContactDetails.faxNumber must be (None)
      etmpContactDetails.mobileNumber must be (None)
      etmpContactDetails.phoneNumber must be (Some("1234567890"))
    }

    "correctly setup EtmpContactDetails if we have no email or phone number" in {
      val testContact = ContactDetails("ABC", "DEF", "")
      val testContactEmail = ContactDetailsEmail(Some(false), "")
      val etmpContactDetails = testRegisterUserService.toEtmpContactDetails(testContact, testContactEmail)
      etmpContactDetails.emailAddress must be (None)
      etmpContactDetails.faxNumber must be (None)
      etmpContactDetails.mobileNumber must be (None)
      etmpContactDetails.phoneNumber must be (None)
    }
  }
}
