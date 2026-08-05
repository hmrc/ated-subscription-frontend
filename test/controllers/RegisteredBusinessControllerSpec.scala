/*
 * Copyright 2026 HM Revenue & Customs
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

import builders.{AuthBuilder, SessionBuilder}
import connectors.AtedConnector
import models.{Address, AtedUsers, BusinessAddress, BusinessCustomerDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EtmpCheckService, RegisteredBusinessService}
import testHelpers.AtedTestHelper
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.HttpResponse
import views.html.{registeredBusinessAddress, registeredWithDifferentGG}

import scala.concurrent.Future

class RegisteredBusinessControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockEtmpCheckService: EtmpCheckService = mock[EtmpCheckService]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val testAddress: Address = Address("line_1", "line_2", None, None, None, "GB")
  val testAddressForm: BusinessAddress = BusinessAddress(Some(true))
  val injectedViewInstance: registeredBusinessAddress = app.injector.instanceOf[views.html.registeredBusinessAddress]
  val injectedViewInstanceAlreadyRegistered: registeredWithDifferentGG = app.injector.instanceOf[views.html.registeredWithDifferentGG]
  val backToBusinessCustomerUrl = "someBackToBusinessCustomerUrl"
  val backToSearchPreviousNrlUrl = "backToSearchPreviousNrlUrl"

  val testRegisteredBusinessController = new RegisteredBusinessController(
    mockMCC,
    mockRegisteredBusinessService,
    mockDataCacheConnector,
    mockBusinessCustomerFrontendConnector,
    mockEtmpCheckService,
    mockAtedConnector,
    mockAuthConnector,
    injectedViewInstance,
    injectedViewInstanceAlreadyRegistered
  )(using mockAppConfig)

  val userId    = "user-8af01429-0927-42d8-b858-e105eb21e9b3"

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
            document.title() must be("Is this where you want us to send any letters about ATED? - Register for ATED - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: ATED registration")
            document.getElementById("registered-business-address-header").text() must include("Is this where you want us to send any letters about ATED?")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be(backToBusinessCustomerUrl)
          }
        }

        "contain title and header as Your correspondence address and no Back Link due to expired link returned from business-customer-frontend" in {
          when(mockAppConfig.backToBusinessCustomerUrl).thenReturn(backToBusinessCustomerUrl)
          withAuthorisedUserAndNoBackLink { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where you want us to send any letters about ATED? - Register for ATED - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: ATED registration")
            document.getElementById("registered-business-address-header").text() must include("Is this where you want us to send any letters about ATED?")
            document.getElementsByClass("govuk-back-link").text() must be("")
            document.getElementsByClass("govuk-back-link").attr("href") must be("")
          }
        }

        "be redirected to nrl page" in {
          when(mockAppConfig.backToSearchPreviousNrlUrl).thenReturn(backToSearchPreviousNrlUrl)
          withAuthorisedUserWithRedirectNRLlink { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where you want us to send any letters about ATED? - Register for ATED - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: ATED registration")
            document.getElementById("registered-business-address-header").text() must include("Is this where you want us to send any letters about ATED?")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("backToSearchPreviousNrlUrl")
          }
        }

        "contain title and header as Your correspondence address for agent registering non-uk client" in {
          withAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where we should send your client’s letters about ATED? - Register for ATED - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: Add a client")
            document.getElementById("registered-business-address-header").text() must include("Is this where we should send your client’s letters about ATED?")
          }
        }

        "should contain address fetched from Keystore" in {
          reset(mockRegisteredBusinessService)
          withAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            val bizAddress = document.select("#businessAddress")

            bizAddress.text() must include("line_1")
            bizAddress.text() must include("line_2")
            bizAddress.text() must include("United Kingdom")

            verify(mockRegisteredBusinessService, times(1)).getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any())
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

        "with already existing User enrolments" in {
          withExistingAtedEnrolledUsers { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.html() must include("test Name has already applied for ATED")
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
          continueWithAuthorisedFormUser(FakeRequest().withMethod("POST").withFormUrlEncodedBody("isCorrespondenceAddress" -> "false")) { result =>
            redirectLocation(result).isDefined must be(true)
            redirectLocation(result).get must include("/ated-subscription/correspondence-address")
          }
        }

        "redirected to the correspondence page if correspondence address is true" in {
          continueWithAuthorisedFormUser(FakeRequest().withMethod("POST").withFormUrlEncodedBody("isCorrespondenceAddress" -> "true")) { result =>
            redirectLocation(result).value must include("/ated-subscription/correspondence-address")
          }
        }

        "return to this page if we have an error" in {
          continueWithAuthorisedFormUser(FakeRequest().withMethod("POST").withFormUrlEncodedBody("isCorrespondenceAddress" -> "1111")) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be(backToBusinessCustomerUrl)
            status(result) must be(BAD_REQUEST)
          }
        }

        "return to this page if we have an error form validation" in {
          continueWithAuthorisedFormUser(FakeRequest().withMethod("POST").withFormUrlEncodedBody("isCorrespondenceAddress" -> "")) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be(backToBusinessCustomerUrl)
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

  val testReviewBusinessDetails: BusinessCustomerDetails = BusinessCustomerDetails(businessName = "test Name", businessType = "LLP",
    businessAddress = testAddress, sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  val testEmptyAtedUsers: AtedUsers = AtedUsers(List(), List())
  val testExistingAtedUsers: AtedUsers = AtedUsers(List("principalUserId1"), List("dlegatedUserId1"))

  private def withAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockBusinessCustomerFrontendConnector.getBackLinkStatus(using any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
    when(mockAtedConnector.checkUsersEnrolments(any())(using any(), any()))
      .thenReturn(Future.successful(Some(testEmptyAtedUsers)))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withAuthorisedUserAndNoBackLink(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockBusinessCustomerFrontendConnector.getBackLinkStatus(using any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "")))
    when(mockAtedConnector.checkUsersEnrolments(any())(using any(), any()))
      .thenReturn(Future.successful(Some(testEmptyAtedUsers)))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withAuthorisedUserWithRedirectNRLlink(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockAtedConnector.checkUsersEnrolments(any())(using any(), any()))
      .thenReturn(Future.successful(Some(testEmptyAtedUsers)))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSessionAndACMUrl(userId))

    test(result)
  }

  private def withExistingAtedEnrolledUsers(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockAtedConnector.checkUsersEnrolments(any())(using any(), any()))
      .thenReturn(Future.successful(Some(testExistingAtedUsers)))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withETMPRegistration(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector, Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("AtedRefNumber", "test")), "Activated")))
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(true))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withAuthorisedUserWithSavedData(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(Some(testAddressForm)))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withAuthorisedAgent(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(using any(), any()))
      .thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testAddress))
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(using any(), any(), any(), any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    when(mockEtmpCheckService.validateBusinessDetails(any())(using any(), any(), any()))
      .thenReturn(Future.successful(false))
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def withUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisteredBusinessController.registeredBusinessAddress.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedFormUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(any())(using any(), any(), any(), any())).thenReturn(Future.successful(testAddress))
    when(mockDataCacheConnector.saveRegisteredBusinessDetails(any[BusinessAddress])(using any(), any())).thenReturn(Future.successful(None))
    val result = testRegisteredBusinessController.continue.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

  private def continueWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisteredBusinessController.continue.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
