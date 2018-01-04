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
import models.{ContactDetails, ContactDetailsEmail}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.ContactDetailsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }

class ContactDetailsEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockContactDetailsService = mock[ContactDetailsService]
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")

  object TestContactDetailsEmailController extends ContactDetailsEmailController {
    override val authConnector = mockAuthConnector
    override val contactDetailsService = mockContactDetailsService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockContactDetailsService)
  }

  "ContactDetailsEmailController" must {
    "not respond with NOT_FOUND" in {
      val result = route(FakeRequest(GET, "/ated-subscription/contact-details-email"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

    "Authorised users" must {

      "respond with OK" in {
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
        getWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Can we use an email address as a point of contact? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/contact-details")

          document.getElementById("contact-details-email.header").text() must be("Can we use an email address as a point of contact?")
          document.getElementById("subtitle").text() must be("This section is: Add a client")
          document.getElementById("lede").text() must be("If we can use email rather than letters there will be less delays in dealing with enquiries.")
          document.getElementById("email-risk-question").text() must be("What are the risks of email and why we need your consent")
          document.getElementById("details-content-0").text() must include("HMRC may need to send emails to you about your ATED account")
          document.getElementById("email-contact-hidden").text() must be("Email address")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "email consent page filled with details after edit" in {
        getWithAuthorisedAgentEdit { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Can we use an email address as a point of contact? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/review-business-details")

          document.getElementById("emailConsent-true").attr("checked") must be("checked")
          document.getElementById("emailConsent-false").attr("checked") must be("")
          document.getElementById("email").attr("value") must be("abc@test.com")

          document.getElementById("submit").text must be("Continue")
        }
      }
      "email consent after edit with no data" in {
        getWithAuthorisedAgentEditNoData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Can we use an email address as a point of contact? - GOV.UK")
          document.getElementById("emailConsent-true").attr("checked") must be("")
          document.getElementById("emailConsent-false").attr("checked") must be("")
          document.getElementById("email").attr("value") must be("")

          document.getElementById("submit").text must be("Continue")
        }
      }


      "Email address must not be more 241 characters" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val emailTest = "a" * 240 + "@mail.com"
        val inputJson = Json.parse( s"""{ "emailConsent": "true", "email": "$emailTest" }""")

        submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("The email address cannot be more than 241 characters.")
        }
      }

      "Email address must be a valid email address" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val inputJson = Json.parse( s"""{  "emailConsent": "true", "email": "abcdef.com" }""")

        submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("The email address is not valid")
        }
      }

      "for valid data, it should redirect to review business details page" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val inputJson = Json.parse( s"""{  "emailConsent": "true", "email": "abcdef@mail.com" }""")
        submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/ated-subscription/review-business-details")
        }
      }
    }

    }




  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgentEdit(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetailsEmail(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
    val result = TestContactDetailsEmailController.editDetailsEmail().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
  def getWithAuthorisedAgentEditNoData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetailsEmail(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestContactDetailsEmailController.editDetailsEmail().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetailsEmail(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))

    val result = TestContactDetailsEmailController.submit(None).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def getEditWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetailsEmail(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
    val result = TestContactDetailsEmailController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }




}
