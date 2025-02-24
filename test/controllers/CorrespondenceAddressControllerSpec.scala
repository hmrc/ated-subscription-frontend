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
import models.Address
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CorrespondenceAddressService
import testHelpers.AtedTestHelper
import views.html.correspondenceAddress
import scala.concurrent.Future


class CorrespondenceAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val testAddress: Address = Address("line_1", "line_2", None, None, None, "GB")
  val injectedViewInstance: correspondenceAddress = app.injector.instanceOf[views.html.correspondenceAddress]

  val testCorrespondenceAddressController: CorrespondenceAddressController = new CorrespondenceAddressController(mockMCC, mockCorrespondenceAddressService, mockAuthConnector, injectedViewInstance, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockCorrespondenceAddressService)
  }

  "CorrespondenceAddressController" must {

    "editAddress" must {
      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "show the correspondence address view" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Where should we send your letters about ATED? - GOV.UK")
            document.getElementById("subtitle").text() must be("This section is: ATED registration")
            document.getElementById("correspondence-address-header").text() must include("Where should we send your letters about ATED?")
            document.getElementById("correspondence-address-lede").text() must be("This can be the address of your authorised agent.")
            document.getElementsByAttributeValue("for", "line_1").text() must be("Address line 1")
            document.getElementById("line_1").text() must be("")
            document.getElementById("line_2").text() must be("")
            document.getElementById("line_3").text() must be("")
            document.getElementById("line_4").text() must be("")
            document.getElementsByAttributeValue("for", "postcode").text() must include("Postcode (optional)")
            document.getElementById("postcode").text() must be("")
            document.getElementsByAttributeValue("for", "country").text() must include("Country")
            document.getElementById("submit").text() must be("Continue")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be ("/ated-subscription/registered-business-address")
          }
        }

        "show the correspondence address view for agent registering non-uk client" in {
          getWithAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Where should we send your client’s letters about ATED? - GOV.UK")
            document.getElementById("subtitle").text() must be("This section is: Add a client")
            document.getElementById("correspondence-address-lede").text() must be("This can be your address as their authorised agent.")
            document.getElementById("correspondence-address-header").text() must include("Where should we send your client’s letters about ATED?")
            document.getElementsByAttributeValue("for", "line_1").text() must be("Address line 1")
            document.getElementById("line_1").text() must be("")
            document.getElementById("line_2").text() must be("")
            document.getElementById("line_3").text() must be("")
            document.getElementById("line_4").text() must be("")
            document.getElementsByAttributeValue("for", "postcode").text() must include("Postcode (optional)")
            document.getElementById("postcode").text() must be("")
            document.getElementsByAttributeValue("for", "country").text() must include("Country")
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "if data exists in keystore, fill the form with that data in the page" in {
          editWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Where should we send your letters about ATED? - GOV.UK")
            document.getElementById("correspondence-address-header").text() must include("Where should we send your letters about ATED?")
            document.getElementById("correspondence-address-lede").text() must be("This can be the address of your authorised agent.")
            document.getElementById("line_1").attr("value") must be("line_1")
            document.getElementById("line_2").attr("value") must be("line_2")
            document.getElementById("line_3").attr("value") must be("")
            document.getElementById("line_4").attr("value") must be("")
            document.getElementById("postcode").attr("value") must be("")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/review-business-details")
            document.getElementById("submit").text() must be("Continue")
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

    }

    "submit" must {

      "Authorised users" must {

        "validate form" must {

          "not be empty" in {
            val inputJson = Json.parse( """{ "line_1": "", "line_2": "", "line_3": "", "line_4": "", "postcode": "", "country": ""}""")

            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Enter address line 1")
              contentAsString(result) must include("Enter address line 2")
              contentAsString(result) must include("Enter a country")
            }
          }

          "If entered, Address line 1 must be maximum of 35 characters" in {
            val line1 = "a" * 36
            val inputJson = Json.parse( s"""{ "line_1": "$line1", "line_2": "", "line_3": "", "line_4": "", "postcode": "", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 1 must not be more than 35 characters")
            }
          }

          "If entered, Address line 2 must be maximum of 35 characters" in {
            val line2 = "a" * 36
            val inputJson = Json.parse( s"""{ "line_1": "", "line_2": "$line2", "line_3": "", "line_4": "", "postcode": "", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 2 must not be more than 35 characters")
            }
          }

          "Address line 3 is optional but if entered, must be maximum of 35 characters" in {
            val line3 = "a" * 36
            val inputJson = Json.parse( s"""{ "line_1": "", "line_2": "", "line_3": "$line3", "line_4": "", "postcode": "", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 3 (optional) must not be more than 35 characters")
            }
          }

          "Address line 4 is optional but if entered, must be maximum of 35 characters" in {
            val line4 = "a" * 36
            val inputJson = Json.parse( s"""{ "line_1": "", "line_2": "", "line_3": "", "line_4": "$line4", "postcode": "", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 4 (optional) must not be more than 35 characters")
            }
          }

          "Postcode is optional but if entered, must be maximum of 10 characters" in {
            val line = "a" * 12
            val inputJson = Json.parse( s"""{ "line_1": "", "line_2": "", "line_3": "", "line_4": "", "postcode": "$line", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter a valid postcode")
            }
          }

          "Postcode is optional but if entered, must be a valid string" in {
            val line = "gh*yuy,"
            val inputJson = Json.parse( s"""{ "line_1": "", "line_2": "", "line_3": "", "line_4": "", "postcode": "$line", "country": ""}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter a valid postcode")
            }
          }

          "Country Code must be selected" in {
            val inputJson = Json.parse( """{ "line_1": "", "line_2": "", "line_3": "", "line_4": "", "postcode": "", "country": ""} """)
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Enter a country")
            }
          }

          "If registration details entered are valid, save and continue button must redirect to contact details page, if mode is not edit" in {
            val inputJson = Json.parse( """{ "line_1": "sadsdf", "line_2": "sdfsdf", "line_3": "asd", "line_4": "asd", "postcode": "AA1 1AA", "country": "GB"}""")
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/ated-subscription/contact-details")
                verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }
          "If registration details entered are valid, save and continue button must redirect to contact details page, if mode is edit" in {
            val inputJson = Json.parse( """{ "line_1": "sadsdf", "line_2": "sdfsdf", "line_3": "asd", "line_4": "asd", "postcode": "AA1 1AA", "country": "GB"}""")
            submitEditWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated-subscription/review-business-details")
                verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }
        }
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
    val result = testCorrespondenceAddressController.submit(None).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.submit(None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    val result = testCorrespondenceAddressController.submit(None).apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def editWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
    val result = testCorrespondenceAddressController.editAddress(mode = Some("edit")).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitEditWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockCorrespondenceAddressService.saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
    val result = testCorrespondenceAddressController.submit(mode = Some("edit")).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }
}
