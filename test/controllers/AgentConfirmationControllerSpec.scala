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
import connectors.BusinessCustomerFrontendConnector
import models.{Address, BusinessCustomerDetails}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.HttpResponse
import utils.Dates
import views.html.agentConfirmation

import java.util.UUID
import scala.concurrent.Future

class AgentConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockBCConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val injectedViewInstance: agentConfirmation = app.injector.instanceOf[views.html.agentConfirmation]
  val testAgentConfirmationController: AgentConfirmationController = new AgentConfirmationController(mockMCC, mockBCConnector, mockAuthConnector, injectedViewInstance, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockBCConnector)
  }

  "AgentConfirmationController" must {

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
            document.title() must be("You have successfully set up this agency for ATED - GOV.UK")
            document.getElementById("agent-reference").text() must be(s"You have successfully set up ACME LTD for ATED on ${Dates.formatDate(LocalDate.now())}")
            document.getElementById("submit").text() must be("Add my ATED clients")
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

        "subscribe to service and view the confirmation page if the registration and refresh worked" in {
          continueWithAuthorisedUser(OK) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9959/mandate/agent/summary"))
          }
        }
      }
    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def getWithAuthorisedUser(businessName: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val reviewDetails = BusinessCustomerDetails(businessName = businessName,
      businessType = "Corporate Body",
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890", agentReferenceNumber = Some("JARN1234567"))

    when(mockBCConnector.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(HttpResponse.apply(OK, Json.toJson(reviewDetails).toString())))

    val result = testAgentConfirmationController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def continueWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithUnAuthenticated(test: Future[Result] => Any) {
    val result = testAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }


  def continueWithAuthorisedUser(refreshStatus: Int)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testAgentConfirmationController.continue.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
