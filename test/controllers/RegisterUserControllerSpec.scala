/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import scala.concurrent.Future

class RegisterUserControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with AtedTestHelper {

  val injectedViewInstanceAlreadyRegistered = app.injector.instanceOf[views.html.alreadyRegistered]
  val injectedViewInstanceRegisterUserConfirmation = app.injector.instanceOf[views.html.registerUserConfirmation]
  val injectedViewInstanceError = app.injector.instanceOf[views.html.global_error]
  val testRegisterUserWithEMACController = new RegisterUserController(mockMCC, mockRegisterUserService, mockAuthConnector,injectedViewInstanceAlreadyRegistered, injectedViewInstanceRegisterUserConfirmation, injectedViewInstanceError, mockAppConfig)
  implicit val hc: HeaderCarrier = HeaderCarrier()

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

          "return to generic error page" in {
            registerWithBadRequest {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("Sorry, there is a problem with the service")
            }
          }

          "return to error page for duplicate users" in {
            registerWithDuplicateUser {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("Somebody has already registered from your organisation")
            }
          }

          "return to error page where Internal Error thrown" in {
            registerWithInvalidUser {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("Sorry, there is a problem with the service")
                document.getElementsByTag("h1").text must be("ated.business-registration.generic.error.title")
                document.getElementsByTag("p").text must be ("ated.business-registration.generic.error.message")
            }
          }

          "return to error page for wrong role users" in {
            registerWithWrongRoleUser {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.title() must be("You must be logged in as an administrator to submit an ATED return")
            }
          }
        }
      }

      "confirmation" must {
        "refresh user-profile and view confirmation page" in {
          confirmationWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("You have successfully registered for ATED")
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
  val userId = s"user-${UUID.randomUUID}"
  val successResponse = SubscribeSuccessResponse(Some("2001-12-17T09:30:47Z"), Some("ABCDEabcde12345"), Some("123456789012345"))

  def registerWithUnAuthorisedUser(test: Future[Result] => Any) {
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def registerWithAuthorisedUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(successResponse))
    when(mockRegisterUserService.enrolAted(eqTo(successResponse), eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(CREATED, enrolResp.toString())))
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithBadRequest(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(successResponse))
    when(mockRegisterUserService.enrolAted(eqTo(successResponse), eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "")))
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithDuplicateUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(successResponse))
    when(mockRegisterUserService.enrolAted(eqTo(successResponse), eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(CONFLICT, "")))
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithWrongRoleUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(successResponse))
    when(mockRegisterUserService.enrolAted(eqTo(successResponse), eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(FORBIDDEN, "")))
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithInvalidUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(successResponse))
    when(mockRegisterUserService.enrolAted(eqTo(successResponse), eqTo(false))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "")))
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def registerWithAuthorisedAgent(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.subscribeAndEnrolForAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.confirmation.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def redirectToAtedWithAuthorisedUser(test: Future[Result] => Any) {
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testRegisterUserWithEMACController.redirectToAted.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}