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
import connectors.BusinessCustomerFrontendConnector
import models.{Address, ReviewDetails}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RegisterUserService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.views.formatting.Dates

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class AgentConfirmationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockRegisterUserService = mock[RegisterUserService]
  val mockBCConnector = mock[BusinessCustomerFrontendConnector]

  object TestAgentConfirmationController extends AgentConfirmationController {
    override val authConnector = mockAuthConnector
    override val registerUserService = mockRegisterUserService
    override val businessCustomerFEConnector = mockBCConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockBCConnector)
  }

  "AgentConfirmationController" must {

    "view" must {
      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated-subscription/agent-confirmation"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
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

      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUser("ACME LTD") { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("You have successfully set up this agency for ATED - GOV.UK")
            document.getElementById("agent-reference").text() must be(s"You have successfully set up ACME LTD for ATED on ${Dates.formatDate(LocalDate.now())}")
            document.getElementById("submit").text() must be("Add my ATED clients")
          }
        }
      }
    }

    "continue" must {
      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated-subscription/agent-confirmation/continue/summary"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

      "unauthorised users" must {
        "respond with a redirect" in {
          continueWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "subscribe to service and view the confirmation page if the registration and refresh worked" in {
          continueWithAuthorisedUser(OK) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9959/mandate/agent/summary"))
          }
        }

        "subscribe to service and view the confirmation page if the registration worked and refresh failed" in {
          continueWithAuthorisedUser(FORBIDDEN) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/logout")
          }
        }
      }
    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def getWithAuthorisedUser(businessName: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val reviewDetails = ReviewDetails(businessName = businessName,
      businessType = Some("corporate body"),
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))

    when(mockBCConnector.getReviewDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(reviewDetails)))))

    val result = TestAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def continueWithAuthorisedUser(refreshStatus: Int)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockRegisterUserService.refreshProfile(Matchers.any())).thenReturn(Future.successful(HttpResponse(refreshStatus)))
    val result = TestAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
