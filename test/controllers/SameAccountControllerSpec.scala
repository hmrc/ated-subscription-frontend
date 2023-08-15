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
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import views.html.{inform, sameAccount}

import scala.concurrent.Future

class SameAccountControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val injectedViewInstanceSameAccount: sameAccount = app.injector.instanceOf[views.html.sameAccount]
  val injectedViewInstanceInform: inform = app.injector.instanceOf[views.html.inform]
  val testSameAccountController: SameAccountController = new SameAccountController(mockMCC, mockAuthConnector, injectedViewInstanceSameAccount, injectedViewInstanceInform, mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
  }

  "SameAccountController" must {

    "viewSameAccount" must {

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
          getWithAuthorisedUser() { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Your company may already have registered for an ATED account - GOV.UK")

            document.getElementsByClass("govuk-back-link").attr("href") must include("previous")
          }
        }
      }
    }

    "viewInform" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUserInform { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUserInform { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUserInform() { result =>
            status(result) must be(OK)

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Inform HMRC as soon as you create a new ATED account - GOV.UK")

            document.getElementsByClass("govuk-back-link").attr("href") must include("existing")
          }
        }
      }
    }

    "toNRLQuestionPage" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUserNRL { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUserNRL { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "respond with OK" in {
          when(mockAppConfig.nrlPath)
            .thenReturn("testnrlurl")

          getWithAuthorisedUserNRL() { result =>
            status(result) must be(SEE_OTHER)
          }
        }
      }
    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testSameAccountController.viewSameAccount().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    val result = testSameAccountController.viewSameAccount().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedUser()(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = testSameAccountController.viewSameAccount().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUserInform(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testSameAccountController.viewInform().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticatedInform(test: Future[Result] => Any): Unit = {
    val result = testSameAccountController.viewInform().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedUserInform()(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = testSameAccountController.viewInform().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUserNRL(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testSameAccountController.toNRLQuestionPage().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticatedNRL(test: Future[Result] => Any): Unit = {
    val result = testSameAccountController.toNRLQuestionPage().apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedUserNRL()(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = testSameAccountController.toNRLQuestionPage().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}
