/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{Address, BusinessAddress, BusinessCustomerDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CorrespondenceAddressService, EtmpCheckService, RegisteredBusinessService}
import testHelpers.AtedTestHelper
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import scala.concurrent.Future

class RegisteredBusinessControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val mockEtmpCheckService: EtmpCheckService = mock[EtmpCheckService]
  val testAddress: Address = Address("line_1", "line_2", None, None, None, "GB")
  val testAddressForm: BusinessAddress = BusinessAddress(Some(true))
  val injectedViewInstance = app.injector.instanceOf[views.html.registeredBusinessAddress]
  val backToBusinessCustomerUrl = "someBackToBusinessCustomerUrl"

  val testRegisteredBusinessController = new RegisteredBusinessController(
    mockMCC,
    mockRegisteredBusinessService,
    mockCorrespondenceAddressService,
    mockDataCacheConnector,
    mockEtmpCheckService,
    mockAuthConnector,
    injectedViewInstance,
    mockAppConfig
  )

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisteredBusinessService)
    reset(mockCorrespondenceAddressService)
  }

  "RegisteredBusinessController" must {

    "registeredBusinessAddress" must {

      "Authorised users" must {

        "respond with OK" in {
          withAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "contain title and header as Your correspondence address" in {
          when(mockAppConfig.backToBusinessCustomerUrl).thenReturn(backToBusinessCustomerUrl)
          withAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where you want us to send any letters about ATED?")
            document.getElementById("business-registered-text").text() must be("This section is: ATED registration")
            document.getElementById("registered-business-address-header").text() must be("Is this where you want us to send any letters about ATED?")
            document.getElementById("backLinkHref").text() must be("Back")
            document.getElementById("backLinkHref").attr("href") must be(backToBusinessCustomerUrl)
          }
        }

        "contain title and header as Your correspondence address for agent registering non-uk client" in {
          withAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where we should send your client’s letters about ATED?")
            document.getElementById("business-registered-text").text() must be("This section is: Add a client")
            document.getElementById("registered-business-address-header").text() must be("Is this where we should send your client’s letters about ATED?")
          }
        }

        "should contain address fetched from Keystore" in {
          withAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            val bizAddress = document.select("#businessAddress")

            bizAddress.text() must include("line_1")
            bizAddress.text() must include("line_2")
            bizAddress.text() must include("United Kingdom")

            verify(mockRegisteredBusinessService, times(1)).getDefaultCorrespondenceAddress(any())(any(), any(), any(), any())
          }
        }

        "contain the correspondence address radio buttons" in {
          withAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".govuk-radios__label").text() must include("Yes")
            document.select(".govuk-radios__label").text() must include("No")
            document.getElementById("isCorrespondenceAddress").outerHtml() must not include "checked"
            document.getElementById("isCorrespondenceAddress-2").outerHtml() must not include "checked"
          }
        }

        "contain the correspondence address radio buttons with saved data" in {
          withAuthorisedUserWithSavedData { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".govuk-radios__label").text() must include("Yes")
            document.select(".govuk-radios__label").text() must include("No")
            document.getElementById("isCorrespondenceAddress").outerHtml() must include("checked")
            document.getElementById("isCorrespondenceAddress-2").outerHtml() must not include "checked"
          }
        }

        "contain a continue button" in {
          withAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "redirect users with existing ETMP registrations to ATED home" in {
          withETMPRegistration { result =>
            redirectLocation(result).get must include("/ated/home")
          }
        }

      }

      "unauthorised users" must {
        "respond with a redirect" in {
          withUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          withUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

    }

    "continue" must {

      "Authorised users" must {

        "redirected to the correspondence page if correspondence address is false" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "false" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
            redirectLocation(result).isDefined must be(true)
            redirectLocation(result).get must include("/ated-subscription/correspondence-address")
          }
        }

        "redirected to the contact details page if correspondence address is true" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "true" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
            redirectLocation(result).isDefined must be(true)
            redirectLocation(result).get must include("/ated-subscription/contact-details")
            verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(any())(any(), any())
          }
        }

        "return to this page if we have an error" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "1111" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("backLinkHref").text() must be("Back")
            document.getElementById("backLinkHref").attr("href") must be(backToBusinessCustomerUrl)
            status(result) must be(BAD_REQUEST)
          }
        }

        "return to this page if we have an error form validation" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("backLinkHref").text() must be("Back")
            document.getElementById("backLinkHref").attr("href") must be(backToBusinessCustomerUrl)
            status(result) must be(BAD_REQUEST)
          }
        }
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          continueWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          continueWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

    }
  }

  val testReviewBusinessDetails = BusinessCustomerDetails(businessName = "test Name", businessType = "LLP",
    businessAddress = testAddress, sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  def withAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def withETMPRegistration(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector, Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("AtedRefNumber", "test")), "Activated")))
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(true))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def withAuthorisedUserWithSavedData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(any(), any()))
      .thenReturn(Future.successful(Some(testAddressForm)))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def withAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def withUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def withUnAuthenticated(test: Future[Result] => Any) {
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(any(), any(), any(), any())).thenReturn(Future.successful(testAddress))
    when(mockCorrespondenceAddressService.saveCorrespondenceAddress(any())(any(), any())).thenReturn(Future.successful(Some(testAddress)))
    val result = testRegisteredBusinessController.continue().apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisteredBusinessController.continue().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testRegisteredBusinessController.continue().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

}
