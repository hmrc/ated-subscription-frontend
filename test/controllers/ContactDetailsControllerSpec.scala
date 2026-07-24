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
import models.ContactDetails
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ContactDetailsService
import testHelpers.AtedTestHelper
import views.html.contactDetails

import scala.concurrent.Future

class ContactDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockContactDetailsService: ContactDetailsService = mock[ContactDetailsService]
  val testContact: ContactDetails = ContactDetails("ABC", "DEF", "1234567890")
  val injectedViewInstance: contactDetails = app.injector.instanceOf[views.html.contactDetails]

  val testContactDetailsController = new ContactDetailsController(mockMCC, mockContactDetailsService, mockAuthConnector, injectedViewInstance, mockAppConfig)

  val sessionId = "session-5510719b-cdd1-44dc-ab1c-b0455be14f5b"
  val userId    = "user-949d7f71-94f3-416b-b497-722f595e51be"
  val token     = "RANDOMTOKEN"

  override def beforeEach(): Unit = {
    reset(mockContactDetailsService)
  }

  "ContactDetailsController" must {

    "Authorised users" must {

      "respond with OK" in {
        getWithAuthorisedUser(None) { result =>
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

      "return add business details view" in {
        getWithAuthorisedUser(None){ result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Who should we contact about ATED? - Register for ATED - GOV.UK")
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/correspondence-address")
          document.getElementById("contact-details.header").text() must include("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: ATED registration")
          document.getElementById("text").text() must be("This can be your authorised agent.")
          document.getElementsByAttributeValue("for", "firstName").text() must be("First name")
          document.getElementsByAttributeValue("for", "lastName").text() must be("Last name")
          document.getElementsByAttributeValue("for", "telephone").text() must be("Telephone number")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "return add business details view for AGENT registering non-uk based clients. Have the correct back link for skip" in {
        getWithAuthorisedAgent(Some("skip")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Who should we contact about ATED? - Register for ATED - GOV.UK")
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/registered-business-address")
          document.getElementById("contact-details.header").text() must include("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: Add a client")
          document.getElementById("text").text() must be("This could be your contact details as their authorised agent.")
          document.getElementsByAttributeValue("for", "firstName").text() must be("First name")
          document.getElementsByAttributeValue("for", "lastName").text() must be("Last name")
          document.getElementsByAttributeValue("for", "telephone").text() must be("Telephone number")
          document.getElementById("submit").text must be("Continue")
        }
      }

      "return add business details view with fields pre-populated in Edit" in {
        getEditWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Who should we contact about ATED? - Register for ATED - GOV.UK")
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/review-business-details")
          document.getElementById("contact-details.header").text() must include("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: ATED registration")
          document.getElementById("text").text() must be("This can be your authorised agent.")
          document.getElementsByAttributeValue("for", "firstName").text() must be("First name")
          document.getElementsByAttributeValue("for", "lastName").text() must be("Last name")
          document.getElementsByAttributeValue("for", "telephone").text() must be("Telephone number")
          document.getElementById("firstName").attr("value") must be("ABC")
          document.getElementById("lastName").attr("value") must be("DEF")
          document.getElementById("telephone").attr("value") must be("1234567890")
          document.getElementById("submit").text must be("Continue")
        }
      }
    }

    "submit" must {

      "validate form" must {

        "not be empty" in {
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "",
              "lastName" -> "",
              "telephone" -> "")
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("Enter a first name")
            contentAsString(result) must include("Enter a last name")
            contentAsString(result) must include("Enter a telephone number")
            contentAsString(result) must not include "The telephone number is not valid"
          }
        }

        "First name must be maximum of 35 characters" in {
          val firstName = "a" * 36
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> firstName,
              "lastName" -> "DEF",
              "telephone" -> "")
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The first name cannot be more than 35 characters")
          }
        }

        "Last name must be maximum of 35 characters" in {
          val lastName = "a" * 36
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> lastName,
              "telephone" -> "")
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The last name cannot be more than 35 characters")
          }
        }

        "Telephone number must not be more than 24 characters" in {
          val telephone = "a" * 25
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> "DEF",
              "telephone" -> telephone)
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number cannot be more than 24 characters")
          }
        }

        "Telephone number must not have invalid characters" in {
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> "DEF",
              "telephone" -> "@@@@@@@@@@@@@@")
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number is not valid")
          }
        }

        "Telephone number must not have lower case letters" in {
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> "DEF",
              "telephone" -> "0191222x123")
          ) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number is not valid")
          }
        }


        "for valid data, it should redirect to review business details page" in {
          submitWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> "DEF",
              "telephone" -> "1234567890")
          ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/contact-details-email")
          }
        }

        "If registration details entered are valid, save and continue button must redirect to review details page, if mode is edit" in {
          submitEditWithAuthorisedFormUserSuccess(FakeRequest().withMethod("POST")
            .withFormUrlEncodedBody(
              "firstName" -> "ABC",
              "lastName" -> "DEF",
              "telephone" -> "1234567890")
          ) { result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated-subscription/review-business-details")
              verify(mockContactDetailsService, times(1)).saveContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          }
        }
      }
    }
  }

  private def getWithAuthorisedUser(mode:Option[String])(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testContactDetailsController.editDetails(mode).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithAuthorisedAgent(mode:Option[String])(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testContactDetailsController.editDetails(mode).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testContactDetailsController.editDetails(None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def submitWithAuthorisedFormUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))

    val result = testContactDetailsController.submit(None).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> token,
      "userId" -> userId)
    )

    test(result)
  }

  private def submitEditWithAuthorisedFormUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {


    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))

    val result = testContactDetailsController.submit(mode = Some("edit")).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> token,
      "userId" -> userId)
    )

    test(result)
  }

  private def getEditWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))
    val result = testContactDetailsController.editDetails(mode = Some("edit")).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
