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

package controllers

import java.util.UUID
import builders.{AuthBuilder, SessionBuilder}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{ContactDetailsService, CorrespondenceAddressService, MandateService, RegisteredBusinessService}
import testHelpers.AtedTestHelper
import views.html.reviewBusinessDetails

import scala.concurrent.Future

class ReviewBusinessDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val mockContactDetailsService: ContactDetailsService = mock[ContactDetailsService]
  val mockMandateService: MandateService = mock[MandateService]

  val testAddress: Address = Address("line_1", "line_2", None, None, None, "GB")
  val testAddress2: Address = Address("line_1", "line_2", Some("line_3"), Some("line_3"), Some("NE1 1AB"), "GB")
  val testIdentification: Identification = Identification(idNumber = "ID123", issuingInstitution = "InstTest", issuingCountryCode = "FR")
  val testReviewBusinessDetails: BusinessCustomerDetails = BusinessCustomerDetails(
    businessName = "test Name",
    businessType = "LLP",
    businessAddress = testAddress,
    sapNumber = "1234567890",
    safeId = "EX0012345678909",
    agentReferenceNumber = None,
    identification = Some(testIdentification)
  )
  val testContact: ContactDetails = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail: ContactDetailsEmail = ContactDetailsEmail(Some(true), "abc@test.com")
  val testContactNoEmail: ContactDetailsEmail = ContactDetailsEmail(Some(false), "")
  val testContactLetter: ContactDetails = ContactDetails("ABC", "DEF", "1234567890")
  val emailAddress: AgentEmail = AgentEmail("test@mail.com")
  val clientDisplayName: ClientDisplayName = ClientDisplayName("client display name")
  val injectedViewInstance: reviewBusinessDetails = app.injector.instanceOf[views.html.reviewBusinessDetails]

  val testReviewDetailsController = new ReviewBusinessDetailsController(
    mockMCC,
    mockRegisteredBusinessService,
    mockCorrespondenceAddressService,
    mockContactDetailsService,
    mockMandateService,
    mockAuthConnector,
    injectedViewInstance,
    mockAppConfig
  )

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisteredBusinessService)
    reset(mockCorrespondenceAddressService)
    reset(mockContactDetailsService)
    reset(mockMandateService)
  }

  "ReviewDetailsController" must {

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
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details-email")
            document.select(".govuk-button").text must be("Confirm and register")
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
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details-email")
            document.select(".govuk-button").text must be("Confirm and continue")
          }
        }

        "contain details fetched from Keystore with editable business detail" in {
          getWithAuthorisedUser (Some(testContact), Some(testContactEmail),
            testReviewBusinessDetails.copy(isBusinessDetailsEditable = true), testAddress = Some(testAddress)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")

            document.getElementById("overseas-tax-reference-label").text() must be("Overseas company registration number")
            document.getElementById("overseas-issuing-country-label").text() must be("Country that issued the number")
            document.getElementById("overseas-issuing-institution-label").text() must be("Institution that issued the number")

            document.getElementById("registered-address").text() must be("line_1 line_2 United Kingdom of Great Britain and Northern Ireland (the)")
            document.getElementById("overseas-id-number").text() must be("ID123")
            document.getElementById("overseas-issuingCountryCode").text() must be("France")
            document.getElementById("overseas-issuingInstitution").text() must be("InstTest")

            document.getElementById("correspondence-address").text() must be("line_1 line_2 United Kingdom of Great Britain and Northern Ireland (the)")
            document.getElementById("correspondence-change").text() must be("Change Where we will send letters about ATED")
            document.getElementById("correspondence-change").attr("href") must be("/ated-subscription/correspondence-address?mode=edit")

            document.getElementById("contact-change").attr("href") must be("/ated-subscription/contact-details?mode=edit")

            document.getElementById("contact-pref").text() must be("abc@test.com")
            document.getElementById("contact-pref-change").attr("href") must be("/ated-subscription/contact-details-email")

            document.getElementById("name").text() must be("ABC DEF")
            document.getElementById("telephone").text() must be("1234567890")
            document.getElementById("agent-email-address-change").attr("href") must be(
              "http://localhost:9959/mandate/agent/email/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("correspondence-change").text() must be("Change Where we will send letters about ATED")
            document.getElementById("client-display-name-change").attr("href") must be(
              "http://localhost:9959/mandate/agent/client-display-name/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("client-display-name").text() must be("client display name")
            document.getElementById("contact-change").text() must be("Change Who we will contact about ATED")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details-email")


            document.getElementById("business-name-change").attr("href") must be("http://localhost:9923/business-customer/register/non-uk-client/ATED/edit?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
            document.getElementById("register-address-change").attr("href") must be("http://localhost:9923/business-customer/register/non-uk-client/ATED/edit?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")

            verify(mockRegisteredBusinessService, times(1)).getBusinessCustomerDetails(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(
              ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockMandateService, times(1)).fetchClientDisplayName(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockMandateService, times(1)).fetchEmailAddress(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          }
        }

        "contain details fetched from Keystore with none editable business detail" in {
          getWithAuthorisedUser (Some(testContact), Some(testContactEmail), testAddress = Some(testAddress)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")

            document.getElementById("business-name-edit") must be(null)
            document.getElementById("register-address-edit") must be(null)
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details-email")

            verify(mockRegisteredBusinessService, times(1)).getBusinessCustomerDetails(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetailsEmail(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockMandateService, times(1)).fetchClientDisplayName(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockMandateService, times(1)).fetchEmailAddress(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          }
        }

        "contain details from Keystore with contact preference of letter" in {
          getWithAuthorisedUser (Some(testContactLetter), Some(testContactNoEmail), testAddress = Some(testAddress2)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("business-name").text() must be("test Name")
            document.getElementById("correspondence-change").attr("href") must be("/ated-subscription/correspondence-address?mode=edit")
            document.getElementById("correspondence-address").text() must be(
              "line_1 line_2 line_3 line_3 NE1 1AB United Kingdom of Great Britain and Northern Ireland (the)")
            document.getElementById("name").text() must be("ABC DEF")
            document.getElementById("telephone").text() must be("1234567890")
            document.getElementById("contact-change").text() must be("Change Who we will contact about ATED")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details-email")

            document.getElementById("contact-pref").text() must be("Not provided")

            verify(mockRegisteredBusinessService, times(1)).getBusinessCustomerDetails(
              ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockCorrespondenceAddressService, times(1)).fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())
            verify(mockContactDetailsService, times(1)).fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
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

  def getWithAuthorisedUser(contactDetails: Option[ContactDetails], contactDetailsEmail: Option[ContactDetailsEmail],
                            reviewDetails: BusinessCustomerDetails = testReviewBusinessDetails,
                            testAddress: Option[Address])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(reviewDetails))
    when(mockContactDetailsService.fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(contactDetails))
    when(mockContactDetailsService.fetchContactDetailsEmail(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(contactDetailsEmail))
    when(mockMandateService.fetchEmailAddress(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(emailAddress)))
    when(mockMandateService.fetchClientDisplayName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(clientDisplayName)))
    val result = testReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    val result = testReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(testAddress)))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockContactDetailsService.fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))
    when(mockContactDetailsService.fetchContactDetailsEmail(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(testContactEmail)))
    when(mockMandateService.fetchEmailAddress(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(emailAddress)))
    when(mockMandateService.fetchClientDisplayName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(clientDisplayName)))
    val result = testReviewDetailsController.reviewDetails.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

}
