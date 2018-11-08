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
import models.{BusinessAddress, ContactDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ContactDetailsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }


class ContactDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockContactDetailsService = mock[ContactDetailsService]
  val testContact = ContactDetails("ABC", "DEF", "1234567890")

  object TestContactDetailsController extends ContactDetailsController {
    override val authConnector = mockAuthConnector
    override val contactDetailsService = mockContactDetailsService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockContactDetailsService)
  }

  "ContactDetailsController" must {

    "not respond with NOT_FOUND" in {
      val result = route(FakeRequest(GET, "/ated-subscription/contact-details"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }

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
          document.title() must be("Who should we contact about ATED? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/correspondence-address")

          document.getElementById("contact-details.header").text() must be("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: ATED registration")
          document.getElementById("text").text() must be("This can be your authorised agent.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("telephone_field").text() must be("Telephone number")

          document.getElementById("submit").text must be("Continue")
        }
      }

      "return add business details view for AGENT registering non-uk based clients. Have the correct back link for skip" in {
        getWithAuthorisedAgent(Some("skip")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Who should we contact about ATED? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/registered-business-address")

          document.getElementById("contact-details.header").text() must be("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: Add a client")
          document.getElementById("text").text() must be("This could be your contact details as their authorised agent.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("telephone_field").text() must be("Telephone number")

          document.getElementById("submit").text must be("Continue")
        }
      }

      "return add business details view with fields pre-populated in Edit" in {
        getEditWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Who should we contact about ATED? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/review-business-details")

          document.getElementById("contact-details.header").text() must be("Who should we contact about ATED?")
          document.getElementById("subtitle").text() must be("This section is: ATED registration")
          document.getElementById("text").text() must be("This can be your authorised agent.")
          document.getElementById("firstName_field").text() must be("First name")
          document.getElementById("lastName_field").text() must be("Last name")
          document.getElementById("telephone_field").text() must be("Telephone number")
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
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "firstName": "","lastName": "", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("You must enter a first name")
            contentAsString(result) must include("You must enter a last name")
            contentAsString(result) must include("You must enter a telephone number")
            contentAsString(result) must not include("The telephone number is not valid")
          }
        }

        "First name must be maximum of 35 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val fname = "a" * 36
          val inputJson = Json.parse( s"""{ "firstName": "$fname", "lastName": "DEF", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The first name cannot be more than 35 characters")
          }
        }

        "First name must be have valid characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val fname = "2121313131"
          val inputJson = Json.parse( s"""{ "firstName": "$fname", "lastName": "DEF", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The first name must only include letters a to z, ampersands (&), apostrophes (‘) and hyphens (-)")
          }
        }


        "First name must NOT be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val fname = ""
          val inputJson = Json.parse( s"""{ "firstName": "$fname", "lastName": "DEF", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)

            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementsByClass("error-notification").html() must include("You must enter a first name")
          }
        }


        "Last name must not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val lname = ""
          val inputJson = Json.parse( s"""{ "firstName": "ABC", "lastName": "$lname", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            val doc = Jsoup.parse(contentAsString(result))
            doc.getElementsByClass("error-notification").html() must include("You must enter a last name")
          }
        }

        "Last name must be have valid characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val fname = "2121313131"
          val inputJson = Json.parse( s"""{ "firstName": "name", "lastName": "$fname", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The last name must only include letters a to z, ampersands (&), apostrophes (‘) and hyphens (-)")
          }
        }

        "Last name must be maximum of 35 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val lname = "a" * 36
          val inputJson = Json.parse( s"""{ "firstName": "ABC", "lastName": "$lname", "telephone": ""}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The last name cannot be more than 35 characters")
          }
        }

        "Telephone number must not be more than 24 characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val tele = "a" * 25
          val inputJson = Json.parse( s"""{ "firstName": "ABC", "lastName": "DEF", "telephone": "$tele"}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number cannot be more than 24 characters")
          }
        }

        "Telephone number must not have invalid characters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "firstName": "ABC", "lastName": "DEF", "telephone": "@@@@@@@@@@@@@@"}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number is not valid")
          }
        }

        "Telephone number must not have lower case letters" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( """{ "firstName": "ABC", "lastName": "DEF", "telephone": "0191222x123"}""")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("The telephone number is not valid")
          }
        }


        "for valid data, it should redirect to review business details page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( s"""{ "firstName": "ABC", "lastName": "DEF", "telephone": "1234567890"}""")
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/ated-subscription/contact-details-email")
          }
        }

        "If registration details entered are valid, save and continue button must redirect to review details page, if mode is edit" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = Json.parse( s"""{ "firstName": "ABC", "lastName": "DEF", "telephone": "1234567890"}""")
          submitEditWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated-subscription/review-business-details")
              verify(mockContactDetailsService, times(1)).saveContactDetails(Matchers.any())(Matchers.any())
          }
        }

      }
    }
  }

  def getWithAuthorisedUser(mode:Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestContactDetailsController.editDetails(mode).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgent(mode:Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestContactDetailsController.editDetails(mode).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestContactDetailsController.editDetails(None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestContactDetailsController.editDetails(None).apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testContact)))

    val result = TestContactDetailsController.submit(None).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitEditWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.saveContactDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testContact)))

    val result = TestContactDetailsController.submit(mode = Some("edit")).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def getEditWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockContactDetailsService.fetchContactDetails(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
    val result = TestContactDetailsController.editDetails(mode = Some("edit")).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
