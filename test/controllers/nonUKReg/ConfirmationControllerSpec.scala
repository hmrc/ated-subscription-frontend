/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.nonUKReg

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import models.{Address, BusinessCustomerDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RegisteredBusinessService
import testHelpers.AtedTestHelper

import scala.concurrent.Future

class ConfirmationControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val injectedViewInstance = app.injector.instanceOf[views.html.nonUKReg.confirmation]
  val testConfirmationController = new ConfirmationController(mockMCC, mockRegisteredBusinessService, mockAuthConnector,injectedViewInstance, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisteredBusinessService)
  }

  val testAddress = Address("line_1", "line_2", None, None, None, "GB")
  val testReviewBusinessDetails = BusinessCustomerDetails(businessName = "test Name", businessType = "LLP", businessAddress = testAddress,
    sapNumber = "1234567890", safeId = "EX0012345678909", agentReferenceNumber = None)

  def viewWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisteredBusinessService.getBusinessCustomerDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(testReviewBusinessDetails))
    val result = testConfirmationController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def continueWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testConfirmationController.continue().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  "ConfirmationController" must {

    "view" must {
      "return confirmation page view" in {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Your client has been successfully added to your ATED clients - GOV.UK")
        }
      }
    }

    "continue" must {
      "redirect agent to ated client summary page in mandate frontend" in {
        continueWithAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }
    }
  }

}
