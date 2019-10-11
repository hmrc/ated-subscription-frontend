/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{Address, BusinessAddress}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CorrespondenceAddressService, RegisteredBusinessService}
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RegisteredBusinessControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testAddressForm = BusinessAddress(Some(true))

  val testRegisteredBusinessController = new RegisteredBusinessController(
    mockMCC,
    mockRegisteredBusinessService,
    mockCorrespondenceAddressService,
    mockDataCacheConnector,
    mockAuthConnector,
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
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
          }
        }

        "contain title and header as Your correspondence address" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where you want us to send any letters about ATED? - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: ATED registration")
            document.getElementById("registered-business-address-header").text() must be("Is this where you want us to send any letters about ATED?")
          }
        }

        "contain title and header as Your correspondence address for agent registering non-uk client" in {
          getWithAuthorisedAgent { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Is this where we should send your client’s letters about ATED? - GOV.UK")
            document.getElementById("business-registered-text").text() must be("This section is: Add a client")
            document.getElementById("registered-business-address-header").text() must be("Is this where we should send your client’s letters about ATED?")
          }
        }

        "should contain address fetched from Keystore" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            val bizAddress = document.select("#businessAddress")

            bizAddress.text() must include("line_1")
            bizAddress.text() must include("line_2")
            bizAddress.text() must include("United Kingdom")

            verify(mockRegisteredBusinessService, times(1)).getDefaultCorrespondenceAddress(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
          }
        }

        "contain the correspondence address radio buttons" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".block-label").text() must include("Yes")
            document.select(".block-label").text() must include("No")
            document.getElementById("isCorrespondenceAddress-true").attr("checked") must be("")
            document.getElementById("isCorrespondenceAddress-false").attr("checked") must be("")
          }
        }

        "contain the correspondence address radio buttons with saved data" in {
          getWithAuthorisedUserWithSavedData { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".block-label").text() must include("Yes")
            document.select(".block-label").text() must include("No")
            document.getElementById("isCorrespondenceAddress-true").attr("checked") must be("checked")
            document.getElementById("isCorrespondenceAddress-false").attr("checked") must be("")
          }
        }

        "contain a continue button" in {
          getWithAuthorisedUser { result =>
            val document = Jsoup.parse(contentAsString(result))
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
            verify(mockCorrespondenceAddressService, times(1)).saveCorrespondenceAddress(Matchers.any())(Matchers.any(), Matchers.any())
          }
        }

        "return to this page if we have an error" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "1111" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }

        "return to this page if we have an error form validation" in {
          val inputJson = Json.parse( """{ "isCorrespondenceAddress": "" }""")
          continueWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) { result =>
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

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedUserWithSavedData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAddressForm)))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testRegisteredBusinessController.registeredBusinessAddress().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def continueWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisteredBusinessService.getDefaultCorrespondenceAddress(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testAddress))
    when(mockCorrespondenceAddressService.saveCorrespondenceAddress(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
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
