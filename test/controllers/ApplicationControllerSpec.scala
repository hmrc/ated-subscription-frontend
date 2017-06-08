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

import connectors.DataCacheConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys}

import scala.concurrent.Future

class ApplicationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ApplicationController" must {

    "unauthorised respond with an OK and load unauthorised page" in {
      val result = controllers.ApplicationController.unauthorised().apply(FakeRequest())
      status(result) must equal(OK)
      contentAsString(result) must include("UNAUTHORISED")
    }

    "Cancel" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in {
        val result = controllers.ApplicationController.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }
    }

    "Cancel redirect to start page" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.redirectToAtedStart().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the ated start page " in {
        val result = controllers.ApplicationController.redirectToAtedStart().apply(FakeRequest())
        redirectLocation(result).get must include("/ated/welcome")
      }
    }

    "Logout" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.logout().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the logout page" in {
        val result = controllers.ApplicationController.logout().apply(FakeRequest())
        redirectLocation(result).get must include("/ated/logout")
      }

    }

    "Keep Alive" must {

      "respond with an OK" in {
        val result = controllers.ApplicationController.keepAlive.apply(FakeRequest())

        status(result) must be(OK)
      }
    }

    "Ated Logout" must {

      "respond with a redirect" in {
        val result = controllers.ApplicationController.redirectToLogout.apply(FakeRequest())
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/ated/logout")
      }
    }

    "unauthorisedAssistant returns OK and unauthorised agent page" in {
      val result = controllers.ApplicationController.unauthorisedAssistant().apply(FakeRequest())
      status(result) must equal(OK)
      contentAsString(result) must include("Unauthorised Assistant Agent")
      contentAsString(result) must include("Only admin agents can enrol for the ATED service")
    }

    "clearCache" must {
      "unauthorised users trying to clear cache" must {
        "respond with a redirect to unauthorized page" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
          val result = TestApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/unauthorised")
        }
      }

      "authorized users clearing cache" must {
        "be able to clear cache successfully" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          when(mockDataCacheConnector.clearCache(Matchers.any())) thenReturn (Future.successful(HttpResponse(200)))
          val result = TestApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(OK)
        }

        "handle error" in {
          val userId = s"user-${UUID.randomUUID}"
          builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
          when(mockDataCacheConnector.clearCache(Matchers.any())) thenReturn (Future.successful(HttpResponse(400)))
          val result = TestApplicationController.clearCache.apply(fakeRequestWithSession(userId))

          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  override def beforeEach = {
    reset(mockDataCacheConnector)
    reset(mockAuthConnector)
  }

  object TestApplicationController extends ApplicationController {
    override val authConnector = mockAuthConnector
    override val dataCacheConnector = mockDataCacheConnector
  }

  private def fakeRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

}
