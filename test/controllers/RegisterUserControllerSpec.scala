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
import models.{EnrolResponse, SubscribeSuccessResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class RegisterUserControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val testRegisterUserWithEMACController = new RegisterUserController(mockMCC, mockRegisterUserService, mockAuthConnector, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
  }

  "RegisterUserController" must {

    "enrolment through EMAC" when {

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

          "return to error page for duplicate users" in {
            registerWithDuplicateUser {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("Somebody has already registered from your organisation - GOV.UK")
            }
          }

          "return to error page for wrong role users" in {
            registerWithWrongRoleUser {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("You must be logged in as an administrator to submit an ATED return - GOV.UK")
            }
          }

          "throw exeception for invalid users" in {
            registerWithInvalidUser {
              result =>
                val thrown = the[RuntimeException] thrownBy redirectLocation(result).get
                thrown.getMessage must include("EMAC Allocate an Enrolment to a Group failed for no definite reason")
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
            document.getElementById("happens-next").text() must be("Use this service to:")
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

  }

  val enrolResp: JsValue = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))

  val badGatewayResponse9001: JsValue = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>9001</ErrorNumber>"}	""")
  val badGatewayResponse11006: JsValue = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>11006</ErrorNumber>"}	""")
  val badGatewayResponse10004: JsValue = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>10004</ErrorNumber>"}	""")
  val badGatewayResponse8026: JsValue = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>8026</ErrorNumber>"}	""")
  val badGatewayResponseOthers: JsValue = Json.parse( """{"statusCode":502,"message":"<ErrorNumber>1234</ErrorNumber>"}	""")

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(CREATED, Some(enrolResp))))
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithDuplicateUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(BAD_REQUEST)))
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithWrongRoleUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(FORBIDDEN)))
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithInvalidUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))
    when(mockRegisterUserService.subscribeAted(Matchers.eq(false))(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(successResponse, HttpResponse(INTERNAL_SERVER_ERROR)))
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val result = testRegisterUserWithEMACController.registerUser.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val result = testRegisterUserWithEMACController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def redirectToAtedWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.redirectToAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }



}
