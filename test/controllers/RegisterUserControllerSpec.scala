/*
 * Copyright 2017 HM Revenue & Customs
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
import config.FrontendAuthConnector
import models.{EnrolResponse, SubscribeSuccessResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{RegisterEmacUserService, RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class RegisterUserControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  val mockAuthConnector = mock[AuthConnector]
  val mockRegisterUserService = mock[RegisterUserService]
  val mockRegisterEMACUserService = mock[RegisterEmacUserService]
  val isEmacFeatureToggle = false

  object TestRegisterUserController extends RegisterUserController {
    val authConnector = mockAuthConnector
    val registerUserService = mockRegisterUserService
    val registerEmacUserService = mockRegisterEMACUserService
    val isEmacFeatureToggle = false
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
  }

  "RegisterUserController" should {

    "use correct connectors" in {
      RegisterUserController.authConnector must be(FrontendAuthConnector)
      RegisterUserController.registerUserService must be(RegisterUserService)
    }

    "not respond with NOT_FOUND for the GET" in {
      val result = route(FakeRequest(POST, "/ated-subscription/register-user"))
      result.isDefined must be(true)
      status(result.get) must not be NOT_FOUND
    }


    "registerUser" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          registerWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          registerWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised Users" must {
        "subscribe to service and redirect to the confirmation page" in {
          registerWithAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/user-confirmation")
          }
        }
        "redirect to the declaration page, if an Agent registers non-uk based client" in {
          registerWithAuthorisedAgent { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/agent/declaration")
          }
        }
      }

      "fail to subcribe a client and redirect to error page" when {

        "different client try to register with same details" in {
          registerWithDuplicateUser(badGatewayResponse9001) { result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) must be(OK)
            document.getElementById("content").text() must include("Somebody has already registered from your organisation")
          }
        }

        "known facts already exist for the client in GG" in {
          registerWithDuplicateUser(badGatewayResponse11006) { result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) must be(OK)
            document.getElementById("content").text() must include("Somebody has already registered from your organisation")
          }
        }

        "multiple enrollments exist for the credential in GG" in {
          registerWithDuplicateUser(badGatewayResponse10004) { result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) must be(OK)
            document.getElementById("content").text() must include("Somebody has already registered from your organisation")
          }
        }

        "users with non-admin role tries to subscribe to ATED" in {
          registerWithDuplicateUser(badGatewayResponse8026) { result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) must be(OK)
            document.getElementById("content").text() must include("You must be logged in as an administrator to submit an ATED return")
          }
        }
      }

      "throw a RuntimeException" when {

        "BAD_GATWAY error is returned but with a different error code" in {
          registerWithDuplicateUser(badGatewayResponseOthers) { result =>
            val thrown = the[RuntimeException] thrownBy redirectLocation(result).get
            thrown.getMessage must include("No matching ErrorNumber from GG Enrolment BAD_GATEWAY Exception")
          }
        }
      }


    }

    "confirmation" must {
      "refresh user-profile and view confirmation page" in {
        confirmationWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("You have successfully registered for ATED - GOV.UK")
          document.getElementById("header").text() must include("You have successfully registered for ATED")
          document.getElementById("happens-next").text() must be("You can:")
          document.getElementById("instruction-1").text() must be("create an ATED return")
          document.getElementById("instruction-2").text() must be("appoint an ATED-registered agent")
          document.getElementById("submit").text() must be("Continue")
        }
      }
    }

    "Redirect To Ated" must {
      "redirect the user to the Ated Page" in {
        redirectToAtedWithAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/ated/home")
        }
      }
    }
  }

  val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))

  val badGatewayResponse9001 = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>9001</ErrorNumber>"}	""")
  val badGatewayResponse11006 = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>11006</ErrorNumber>"}	""")
  val badGatewayResponse10004 = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>10004</ErrorNumber>"}	""")
  val badGatewayResponse8026 = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>8026</ErrorNumber>"}	""")
  val badGatewayResponseOthers = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>1234</ErrorNumber>"}	""")

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestRegisterUserController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestRegisterUserController.registerUser.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(OK, Some(enrolResp))))
    val result = TestRegisterUserController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithDuplicateUser(badGatewayResponse: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(BAD_GATEWAY, Some(badGatewayResponse))))
    val result = TestRegisterUserController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def registerWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val result = TestRegisterUserController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockRegisterUserService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    val result = TestRegisterUserController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def redirectToAtedWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestRegisterUserController.redirectToAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
