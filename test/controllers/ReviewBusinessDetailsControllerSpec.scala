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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ContactDetailsService, CorrespondenceAddressService, MandateService, RegisteredBusinessService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.contactDetailsEmail

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ReviewBusinessDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockRegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService = mock[CorrespondenceAddressService]
  val mockContactDetailsService = mock[ContactDetailsService]
  val mockMandateService = mock[MandateService]

  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testAddress2 = Address("line_1", "line_2", Some("line_3"), Some("line_3"), Some("NE1 1AB"), "GB")
  val testIdentification = Identification(idNumber = "ID123", issuingInstitution = "InstTest", issuingCountryCode = "FR")
  val testReviewBusinessDetails = ReviewDetails(businessName = "test Name", businessType = Some("test Type"), businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None,
    identification = Some(testIdentification)
  )
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")
  val testContactNoEmail = ContactDetailsEmail(Some(false), "")
  val testContactLetter = ContactDetails("ABC", "DEF", "1234567890")
  val emailAddress = AgentEmail("test@mail.com")
  val clientDisplayName = ClientDisplayName("client display name")

  object TestReviewDetailsController extends ReviewBusinessDetailsController {
    override val authConnector = mockAuthConnector
    override val registeredBusinessService = mockRegisteredBusinessService
    override val correspondenceAddressService = mockCorrespondenceAddressService
    override val contactDetailsService = mockContactDetailsService
    override val mandateService = mockMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisteredBusinessService)
    reset(mockCorrespondenceAddressService)
    reset(mockContactDetailsService)
    reset(mockMandateService)
  }

  "ReviewDetailsController" must {

    "not respond with NOT_FOUND for the GET" in {
      val result = route(FakeRequest(GET, "/ated-subscription/review-business-details"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

    "reviewDetails" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised Users" must {
        "return review business details view" in {

          getWithAuthorisedUser (Some(testContact), Some(testContactEmail), testAddress = Some(testAddress)){ result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Check your business details - GOV.UK")
            document.getElementById("business-name-label").text() must be("Business name")
            document.getElementById("business-address-label").text() must be("Registered address")
            document.getElementById("overseas-tax-reference-label") must be(null)
            document.getElementById("overseas-issuing-country-label") must be(null)
            document.getElementById("overseas-issuing-institution-label") must be(null)
            document.getElementById("correspondence-address-label").text() must be("Where we will send letters about ATED")
            document.getElementById("contact-details-label").text() must be("Who we will contact about ATED")
            document.getElementById("email-address-label").text() must be("Agent’s email address")
            document.getElementById("contact-pref-label").text() must be("Email address")
            document.getElementById("client-display-name-label").text() must be("Display name")
            document.select(".button").text must be("Confirm and register")
          }
        }

        "return review business details view for agents registering non-uk clients" in {

          getWithAuthorisedAgent { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Check your client’s ATED details are correct - GOV.UK")
            document.getElementById("business-name-label").text() must be("Business name")
            document.getElementById("business-address-label").text() must be("Registered address")
            document.getElementById("overseas-tax-reference-label") must be(null)
            document.getElementById("overseas-issuing-country-label") must be(null)
            document.getElementById("overseas-issuing-institution-label") must be(null)

            document.getElementById("correspondence-address-label").text() must be("Where we will send letters about ATED")
            document.getElementById("contact-pref-label").text() must be("Email address")
            document.getElementById("contact-details-label").text() must be("Who we will contact about ATED")
            document.select(".button").text must be("Confirm and continue")
          }
        }

        "contain details fetched from Keystore with editable business detail" in {
          getWithAuthorisedUser (Some(testContact), Some(testContactEmail), testReviewBusinessDetails.copy(isBusinessDetailsEditable = true), testAddress = Some(testAddress)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")

            document.getElementById("overseas-tax-reference-label").text() must be("Overseas company registration number")
            document.getElementById("overseas-issuing-country-label").text() must be("Country that issued the number")
            document.getElementById("overseas-issuing-institution-label").text() must be("Institution that issued the number")

            document.getElementById("registered-address").text() must be("line_1 line_2 United Kingdom")
            document.getElementById("overseas-id-number").text() must be("ID123")
            document.getElementById("overseas-issuingCountryCode").text() must be("France")
            document.getElementById("overseas-issuingInstitution").text() must be("InstTest")

            document.getElementById("correspondence-address").text() must be("line_1 line_2 United Kingdom")
            document.getElementById("correspondence-edit").text() must be("Edit Where we will send letters about ATED")
            document.getElementById("correspondence-edit").attr("href") must be("/ated-subscription/correspondence-address?mode=edit")

            document.getElementById("contact-edit").attr("href") must be("/ated-subscription/contact-details?mode=edit")

            document.getElementById("contact-pref").text() must be("abc@test.com")
            document.getElementById("contact-pref-edit").attr("href") must be("/ated-subscription/contact-details-email-edit")

            document.getElementById("name").text() must be("ABC DEF")
            document.getElementById("telephone").text() must be("1234567890")
            document.getElementById("agent-email-address-edit").attr("href") must be("http://localhost:9959/mandate/agent/email/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("correspondence-edit").text() must be("Edit Where we will send letters about ATED")
            document.getElementById("client-display-name-edit").attr("href") must be("http://localhost:9959/mandate/agent/client-display-name/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("client-display-name").text() must be("client display name")
            document.getElementById("contact-edit").text() must be("Edit Who we will contact about ATED")


            document.getElementById("business-name-edit").attr("href") must be("http://localhost:9923/business-customer/register/non-uk-client/ATED/edit?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("register-address-edit").attr("href") must be("http://localhost:9923/business-customer/register/non-uk-client/ATED/edit?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")

            verify(mockRegisteredBusinessService, times(1)).getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(Matchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(Matchers.any())
            verify(mockMandateService, times(1)).fetchClientDisplayName(Matchers.any(), Matchers.any())
            verify(mockMandateService, times(1)).fetchEmailAddress(Matchers.any(), Matchers.any())
          }
        }

        "contain details fetched from Keystore with none editable business detail" in {
          getWithAuthorisedUser (Some(testContact), Some(testContactEmail), testAddress = Some(testAddress)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")

            document.getElementById("business-name-edit") must be(null)
            document.getElementById("register-address-edit") must be(null)

            verify(mockRegisteredBusinessService, times(1)).getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(Matchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(Matchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetailsEmail(Matchers.any())
            verify(mockMandateService, times(1)).fetchClientDisplayName(Matchers.any(), Matchers.any())
            verify(mockMandateService, times(1)).fetchEmailAddress(Matchers.any(), Matchers.any())
          }
        }

        "contain details from Keystore with contact preference of letter" in {
          getWithAuthorisedUser (Some(testContactLetter), Some(testContactNoEmail), testAddress = Some(testAddress2)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")
            document.getElementById("correspondence-edit").attr("href") must be("/ated-subscription/correspondence-address?mode=edit")
            document.getElementById("correspondence-address").text() must be("line_1 line_2 line_3 line_3 NE1 1AB United Kingdom")
            document.getElementById("name").text() must be("ABC DEF")
            document.getElementById("telephone").text() must be("1234567890")
            document.getElementById("contact-edit").text() must be("Edit Who we will contact about ATED")

            document.getElementById("contact-pref").text() must be("Not provided")

            verify(mockRegisteredBusinessService, times(1)).getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(Matchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(Matchers.any())
          }
        }
        "not contain correspondence address" in {
          getWithAuthorisedUser (contactDetails = Some(testContactLetter),contactDetailsEmail = None, testAddress = None) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("Correspondence Address not found!")
          }
        }
        "not contain contact details" in {
          getWithAuthorisedUser (contactDetails = None,contactDetailsEmail = None, testAddress = Some(testAddress)) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("Contact Details not found!")
          }
        }
      }
    }

  }

  def getWithAuthorisedUser(contactDetails: Option[ContactDetails],contactDetailsEmail: Option[ContactDetailsEmail], reviewDetails: ReviewDetails = testReviewBusinessDetails, testAddress: Option[Address])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(reviewDetails))
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(contactDetails))
    when(mockContactDetailsService.fetchContactDetailsEmail(Matchers.any())).thenReturn(Future.successful(contactDetailsEmail))
    when(mockMandateService.fetchEmailAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(emailAddress)))
    when(mockMandateService.fetchClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(clientDisplayName)))
    val result = TestReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
    when(mockRegisteredBusinessService.getReviewBusinessDetails(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
    when(mockContactDetailsService.fetchContactDetailsEmail(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
    when(mockMandateService.fetchEmailAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(emailAddress)))
    when(mockMandateService.fetchClientDisplayName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(clientDisplayName)))
    val result = TestReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

}
