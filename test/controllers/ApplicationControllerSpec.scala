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

import connectors.AtedSubscriptionDataCacheConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}

import scala.concurrent.Future

class ApplicationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach: Unit = {
    reset(mockDataCacheConnector)
    reset(mockAuthConnector)
  }

  val testApplicationController = new ApplicationController(mockMCC, mockDataCacheConnector, mockAuthConnector, mockAppConfig)

  private def fakeRequestWithSession(userId: String): FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }



  "ApplicationController" must {

    "unauthorised respond with an OK and load unauthorised page" in {
      val result = testApplicationController.unauthorised().apply(FakeRequest())
      status(result) must equal(OK)
      contentAsString(result) must include("UNAUTHORISED")
    }

    "Cancel" must {

      "respond with a redirect" in {
        val result = testApplicationController.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in {
        val result = testApplicationController.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }
    }

    "Cancel redirect to start page" must {

      "respond with a redirect" in {
        val result = testApplicationController.redirectToAtedStart().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the ated start page " in {
        val result = testApplicationController.redirectToAtedStart().apply(FakeRequest())
        redirectLocation(result).get must include("/ated/home")
      }
    }

    "Logout" must {

      "respond with a redirect" in {
        val result = testApplicationController.logout().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the logout page" in {
        val result = testApplicationController.logout().apply(FakeRequest())
        redirectLocation(result).get must include("/ated/logout")
      }

    }

    "Keep Alive" must {

      "respond with an OK" in {
        val result = testApplicationController.keepAlive.apply(FakeRequest())

        status(result) must be(OK)
      }
    }

    "Ated Logout" must {

      "respond with a redirect" in {
        val result = testApplicationController.redirectToLogout.apply(FakeRequest())
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/ated/logout")
      }
    }

    "unauthorisedAssistant returns OK and unauthorised agent page" in {
      val result = testApplicationController.unauthorisedAssistant().apply(FakeRequest())
      status(result) must equal(OK)
      contentAsString(result) must include("Unauthorised Assistant Agent")
      contentAsString(result) must include("Only admin agents can enrol for the ATED service")
    }

    "clearCache" must {
      "unauthorised users trying to clear cache" must {
        "respond with a redirect to unauthorized page" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
          val result = testApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/unauthorised")
        }
      }

      "authorized users clearing cache" must {
        "be able to clear cache successfully" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          when(mockDataCacheConnector.clearCache(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(200))
          val result = testApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(OK)
        }

        "handle error" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          when(mockDataCacheConnector.clearCache(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(400))
          val result = testApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}
