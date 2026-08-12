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

import builders.{AuthBuilder, SessionBuilder}
import models.Address
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Play.materializer
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CorrespondenceAddressService
import services.RegisteredBusinessService
import views.html.correspondenceAddress

import scala.concurrent.Future
import play.api.test.Helpers.defaultAwaitTimeout
import testHelpers.AtedTestHelper

class CorrespondenceAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val injectedViewInstance: correspondenceAddress = app.injector.instanceOf[views.html.correspondenceAddress]

  val testCorrespondenceAddressController: CorrespondenceAddressController = new CorrespondenceAddressController(mockMCC, mockCorrespondenceAddressService,
    mockRegisteredBusinessService, mockAuthConnector, injectedViewInstance)(using mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockCorrespondenceAddressService)
  }

  val userId = "user-ad1235e7-3518-4c5a-b8b8-6e5d08c0c1e2"

  val emptyAddressLine = ""
  val anAddressLine_1 = "Adddress line 1"
  val anAddressLine_2 = "Adddress line 2"
  val anAddressLine_3 = "Adddress line 3"
  val anAddressLine_4 = "Adddress line 4"
  val aTooLongAddressLine = "a" * 36
  val aPostcode= "AA1 1AA"
  val emptyPostcode = ""
  val aCountryCode = "GB"
  val emptyCountryCode = ""

  val testAddress: Address = Address(
    anAddressLine_1,
    anAddressLine_2,
    None,
    None,
    None,
    aCountryCode)

  "CorrespondenceAddressController" must {

    "editAddress" must {
      "Authorised users" must {

        "respond with OK" in {
          when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

          getWithAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "show the correspondence address view" in {
          when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Where should we send your letters about ATED? - Register for ATED - GOV.UK")
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
            document.select("#country option[selected]").`val`() must be("")
            document.getElementById("submit").text() must be("Continue")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be ("/ated-subscription/registered-business-address")
          }
        }

        "show the correspondence address view for agent registering non-uk client" in {
          when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

          getWithAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Where should we send your client’s letters about ATED? - Register for ATED - GOV.UK")
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
            document.select("#country option[selected]").`val`() must be("")
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "if data exists in keystore, fill the form with that data in the page" in {
          when(mockCorrespondenceAddressService.fetchCorrespondenceAddress(using ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Some(testAddress)))

          editWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Where should we send your letters about ATED? - Register for ATED - GOV.UK")
            document.getElementById("correspondence-address-header").text() must include("Where should we send your letters about ATED?")
            document.getElementById("correspondence-address-lede").text() must be("This can be the address of your authorised agent.")
            document.getElementById("line_1").attr("value") must be(anAddressLine_1)
            document.getElementById("line_2").attr("value") must be(anAddressLine_2)
            document.getElementById("line_3").attr("value") must be(emptyAddressLine)
            document.getElementById("line_4").attr("value") must be(emptyAddressLine)
            document.getElementById("postcode").attr("value") must be(emptyPostcode)
            document.select("#country option[selected]").`val`() must be(aCountryCode)
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
            submitWithAuthorisedFormUserSuccess(
              FakeRequest(POST, "/").withFormUrlEncodedBody(
                "line_1" ->  emptyAddressLine,
                "line_2"->   emptyAddressLine,
                "line_3"->   emptyAddressLine,
                "line_4"->   emptyAddressLine,
                "postcode"-> aPostcode,
                "country"->  emptyCountryCode)
            ) { result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Enter address line 1")
                contentAsString(result) must include("Enter address line 2")
                contentAsString(result) must include("Enter a country")
            }
          }

          "If entered, Address line 1 must be maximum of 35 characters" in {
            submitWithAuthorisedFormUserSuccess(
              FakeRequest(POST, "/").withFormUrlEncodedBody(
                "line_1" ->   aTooLongAddressLine,
                "line_2" ->   anAddressLine_2,
                "line_3" ->   anAddressLine_3,
                "line_4" ->   anAddressLine_3,
                "postcode" -> aPostcode,
                "country" ->  aCountryCode)
            ) { result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 1 must not be more than 35 characters")
            }
          }

          "If entered, Address line 2 must be maximum of 35 characters" in {
            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   aTooLongAddressLine,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 2 must not be more than 35 characters")
            }
          }

          "Address line 3 is optional but if entered, must be maximum of 35 characters" in {
            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   aTooLongAddressLine,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 3 (optional) must not be more than 35 characters")
            }
          }

          "Address line 4 is optional but if entered, must be maximum of 35 characters" in {
            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   aTooLongAddressLine,
              "postcode" -> aPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Address line 4 (optional) must not be more than 35 characters")
            }
          }

          "Postcode is optional but if entered, must be maximum of 10 characters" in {
            val aTooLongPostCode = "a" * 11

            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aTooLongPostCode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter a valid postcode")
            }
          }

          "Postcode is optional but if entered, must be a valid string" in {
            val invalidPostcode = "gh*yuy,"

            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> invalidPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("You must enter a valid postcode")
            }
          }

          "Postcode is optional but if entered, it can contain the allowed special characters" in {
            val validPostcode = "{[(ZZ1-1Z Z)]}."

            when(mockCorrespondenceAddressService.saveCorrespondenceAddress(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> validPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/ated-subscription/contact-details")
            }
          }

          "Country Code must be selected" in {
            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aPostcode,
              "country" ->  emptyCountryCode)
            ) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("Enter a country")
            }
          }

          "If registration details entered are valid, save and continue button must redirect to contact details page, if mode is not edit" in {
            when(mockCorrespondenceAddressService.saveCorrespondenceAddress(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))

            submitWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aPostcode,
              "country" ->  aCountryCode)
            ) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated-subscription/contact-details")
                verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }

          "If registration details entered are valid, save and continue button must redirect to contact details page, if mode is edit" in {
            when(mockCorrespondenceAddressService.saveCorrespondenceAddress(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))

            submitEditWithAuthorisedFormUserSuccess(FakeRequest(POST, "/").withFormUrlEncodedBody(
              "line_1" ->   anAddressLine_1,
              "line_2" ->   anAddressLine_2,
              "line_3" ->   anAddressLine_3,
              "line_4" ->   anAddressLine_4,
              "postcode" -> aPostcode,
              "country" ->  aCountryCode)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include("/ated-subscription/review-business-details")
                verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(ArgumentMatchers.any())(using ArgumentMatchers.any(), ArgumentMatchers.any())
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

  private def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.editAddress(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def submitWithAuthorisedFormUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.submit(None).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  private def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.submit(None).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def editWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.editAddress(mode = Some("edit")).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def submitEditWithAuthorisedFormUserSuccess(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testCorrespondenceAddressController.submit(mode = Some("edit")).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }
}
