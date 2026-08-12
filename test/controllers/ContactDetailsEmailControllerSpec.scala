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
import forms.AtedForms.emailLength
import models.ContactDetailsEmail
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.ContactDetailsService
import testHelpers.AtedTestHelper
import views.html.contactDetailsEmail

import scala.concurrent.Future

class ContactDetailsEmailControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with AtedTestHelper {

  val mockContactDetailsService: ContactDetailsService = mock[ContactDetailsService]
  val testContactEmail: ContactDetailsEmail = ContactDetailsEmail(Some(true), "abc@test.com")
  val injectedViewInstance: contactDetailsEmail = app.injector.instanceOf[views.html.contactDetailsEmail]

  val testContactDetailsEmailController: ContactDetailsEmailController = new ContactDetailsEmailController(
    mockMCC, mockContactDetailsService, mockAuthConnector, injectedViewInstance)(using mockAppConfig)

  val sessionId = "session-67828c32-775c-4483-8167-9b54b2ef8605"
  val userId    = "user-fcd7129a-5cd4-4616-95d2-8d673d650fa8"
  val token     = "RANDOMTOKEN"

  "ContactDetailsEmailController" must {

    "Authorised users" must {

      "respond with OK" in {
        when(mockContactDetailsService.fetchContactDetailsEmail(using ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(testContactEmail)))
        getWithAuthorisedAgent { result =>
          status(result) must be(OK)
        }
      }
    }

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

      "email consent page" in {
        when(mockContactDetailsService.fetchContactDetailsEmail(using ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(testContactEmail)))
        getWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Can we use an email address as a point of contact? - Register for ATED - GOV.UK")
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/contact-details?mode=skip")
          document.getElementById("contact-details-email.header").text() must include("Can we use an email address as a point of contact?")
          document.getElementById("subtitle").text() must be("This section is: Add a client")
          document.getElementById("lede").text() must be("If we can use email rather than letters there will be less delays in dealing with enquiries.")
          document.getElementById("email-risk-question").text() must be("What are the risks of email and why we need your consent")
          document.getElementById("details-content-0").text() must include("HMRC may need to send emails to you about your ATED account")
          document.getElementsByAttributeValue("for", "email").text() must be("Email address")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "email consent page filled with details after edit" in {
        when(mockContactDetailsService.fetchContactDetailsEmail(using ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(testContactEmail)))
        getWithAuthorisedAgentEdit { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Can we use an email address as a point of contact? - Register for ATED - GOV.UK")
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/review-business-details")
          document.getElementById("emailConsent-2").outerHtml() must not include "checked"
          document.getElementById("emailConsent").attr("checked") must be("")
          document.getElementById("email").attr("value") must be("abc@test.com")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "email consent after edit with no data" in {
        getWithAuthorisedAgentEditNoData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Can we use an email address as a point of contact? - Register for ATED - GOV.UK")
          document.getElementById("emailConsent").attr("checked") must be("")
          document.getElementById("emailConsent-2").attr("checked") must be("")
          document.getElementById("email").attr("value") must be("")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "Email addresses must not contain more than the allowed number of characters" in {
        val emailTest = "a" * (emailLength - "@mail.com".length + 1) + "@mail.com"
        submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
          .withFormUrlEncodedBody("emailConsent" -> "true", "email" -> emailTest)) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("The email address cannot be more than 132 characters.")
        }
      }

      "Email address must be a valid email address" in {
        submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
          .withFormUrlEncodedBody("emailConsent" -> "true", "email" -> "abcdef.com")) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Enter a valid email address")
        }
      }

      "Email address must be filled" in {
        submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
          .withFormUrlEncodedBody("emailConsent" -> "true", "email" -> "")) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Enter an email address")
        }
      }

      "Question must be answered" in {
        submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
          .withFormUrlEncodedBody("emailConsent" -> "", "email" -> "")) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Select yes if we can use an email address as a point of contact")
        }
      }

      "for valid data, it should redirect to review business details page" in {
        submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
          .withFormUrlEncodedBody("emailConsent" -> "true", "email" -> "abcdef@mail.com")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/ated-subscription/review-business-details")
        }
      }
    }

    }

  private def getWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithAuthorisedAgentEdit(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetailsEmail(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(testContactEmail)))
    val result = testContactDetailsEmailController.editDetailsEmail.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithAuthorisedAgentEditNoData(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetailsEmail(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testContactDetailsEmailController.editDetailsEmail.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def submitWithAuthorisedFormUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetailsEmail(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(testContactEmail)))

    val result = testContactDetailsEmailController.submit(None).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> token,
      "userId" -> userId)
    )

    test(result)
  }
}
