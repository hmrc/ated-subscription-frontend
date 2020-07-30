/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.BusinessCustomerFrontendConnector
import models.PreviousSubmittedForm
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.OverseasCompanyService
import testHelpers.AtedTestHelper

import scala.concurrent.Future

class PreviousSubmittedControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockBCConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockOverseasService: OverseasCompanyService = mock[OverseasCompanyService]
  val injview = app.injector.instanceOf[views.html.previous_submitted]
  val testPreviousSubmittedController: PreviousSubmittedController = new PreviousSubmittedController(mockMCC, mockBCConnector, mockOverseasService, mockAuthConnector, injview, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockBCConnector)
    reset(mockOverseasService)
  }

  "PreviousSubmittedController" must {

    "view" must {

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

      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUser("ACME LTD") { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Has this company submitted ATED returns before? - GOV.UK")
          }
        }

        "respond with OK with prepopped data" in {
          getWithAuthorisedUser("ACME LTD", Some(PreviousSubmittedForm(Some(true)))) { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Has this company submitted ATED returns before? - GOV.UK")
          }
        }
      }
    }

    "continue" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          continueWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "go to the existing user page if they have previously submitted ated" in {
          val inputForm = Seq(("previousSubmitted", "true"))

          continueWithAuthorisedUser(inputForm) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated-subscription/existing"))
          }
        }

        "go to the non-resident landlord page if they have not previously submitted" in {
          val inputForm = Seq(("previousSubmitted", "false"))

          when(mockAppConfig.nrlPath).thenReturn("test")

          continueWithAuthorisedUser(inputForm) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("test"))
          }
        }

        "bad request if the form data is invalid" in {
          val inputForm = Seq(("previousSubmitted", "invalid_data"))

          continueWithAuthorisedUser(inputForm) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }
      }
    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testPreviousSubmittedController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testPreviousSubmittedController.view().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def getWithAuthorisedUser(businessName: String, data: Option[PreviousSubmittedForm] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockOverseasService.fetchPreviouslySubmitted(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(data))

    val result = testPreviousSubmittedController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testPreviousSubmittedController.continue.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testPreviousSubmittedController.continue.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def continueWithAuthorisedUser(inputForm: Seq[(String, String)])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockOverseasService.savePreviouslySubmitted(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))

    val result = testPreviousSubmittedController.continue.apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputForm: _*))

    test(result)
  }
}
