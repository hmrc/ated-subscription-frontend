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
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


class SubscriptionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  object TestSubscriptionController extends SubscriptionController {
    val authConnector = mockAuthConnector
  }

  object TestSubscriptionControllerWhiteListing extends SubscriptionController {
    val authConnector = mockAuthConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "SubscriptionController" must {

    "Start subscription" must {
      "respond without NOT FOUND for the user link " in {

        val result = route(FakeRequest(GET, "/ated-subscription/start-subscription"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "respond without NOT FOUND for the agent link " in {

        val result = route(FakeRequest(GET, "/ated-subscription/start-agent-subscription"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
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

      "unauthorised agent" must {
        "respond with a redirect" in {
          getWithUnAuthorisedAgent { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedAgent { result =>
            redirectLocation(result).get must include("/ated-subscription/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "respond with OK" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
          }
        }

        "return the client subscription landing page view" in {

          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be("Are you an agent acting for a client?")
              document.getElementById("client-startpage-header").text() must be("Are you an agent acting for a client?")
              document.getElementById("submit").text() must be("Continue")
          }
        }

      }

      "Authorised agent" must {
        "respond with OK" in {
          getWithAuthorisedAgent {
            result =>
              status(result) must be(OK)
          }
        }

        "assistant agent must be redirected to unauthorised agent assistant page" in {
          getWithAuthorisedAgentAssistant {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated-subscription/unauthorised-assistant"))
          }
        }

        "respond with Redirect to the Agent page" in {
          getWithAuthorisedAgentThroughUserLink {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/start-agent-subscription")
          }
        }

        "return the agent subscription landing page view" in {
          getWithAuthorisedAgent {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Set up your agency for the new ATED online service")
              document.getElementById("subtitle").text() must be("This section is: ATED agency set up")
              document.getElementById("agent-startpage-header").text() must be("Set up your agency for the new ATED online service")
              document.getElementById("lede-paragraph").text() must include("Before you can submit ATED returns on behalf of your clients you must set up your agency")
              document.getElementById("agent-startpage-text1").text() must be("Enter your agency's registered name and Unique Taxpayer Reference (UTR).")
              document.getElementById("agent-startpage-text2").text() must be("After setting up your agency, you can add your clients.")
              document.getElementById("agent-startpage-text1a").text() must be("You must:")
              document.getElementById("submit").text() must be("Set up your agency")

          }
        }
      }

      "Unauthenticated users" must {
        "respond with a redirect" in {
          submitWithUnAuthenticated { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          submitWithUnAuthenticated { result =>
            redirectLocation(result).get must include("/gg/sign-in")
          }
        }

        "be redirected to unauthorised" in {
          subscribeAgentWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
      }

      "appoint" must {

        "respond with OK" in {
          appointWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("client-startpage-header").text() must be("Do you want to appoint an agent to act for you?")
            document.getElementById("appoint-agent-title").text() must be("To appoint an agent you must:")
            document.getElementById("appoint-agent-text1").text() must be("make sure your agent has set up their agency for ATED and given you their unique authorisation number")
            document.getElementById("appoint-agent-text2").text() must be("register to use the new ATED online service")
            document.getElementById("appoint-agent-text3").text() must be("enter the unique authorisation number when asked")
            document.getElementById("appoint-agent-title2").text() must be("You must:")
            document.getElementById("appoint-agent-text5").text() must be("register to use the new ATED online service")
            document.getElementById("appoint-agent-text6").text() must be("select the correct relief for your property or tell us about a property that is liable for an ATED charge")
            document.getElementById("appoint-agent-text7").text() must be("confirm and submit the relief declaration or ATED return")

          }
        }

      }


      "Continue" must {

//        "respond with BadRequest, if yes was selected" in {
//          val inputForm = Seq(("isAgent", "true"))
//          submitWithAuthorisedUser(inputForm) { result =>
//            val document = Jsoup.parse(contentAsString(result))
//            document.getElementById("isAgent-error").text() must be("There is a problem with the agent question")
//            document.getElementById("isAgent-error-0").text() must be("You must sign in with your agent details if you are an agent")
//            document.getElementById("hidden-isAnAgent").text() must include("If you are an agent acting for a client you need to sign in using your agent Government Gateway details.")
//            document.getElementById("hidden-isAnAgent").getElementsByTag("a").first().attr("href") must be("http://localhost:9025/gg/sign-in?continue=http://localhost:9933/ated-subscription/start-subscription")
//            status(result) must be(BAD_REQUEST)
//          }
//        }

        "respond with redirect, if no is selected" in {
          val inputForm = Seq(("isAgent", "false"))
          submitWithAuthorisedUser(inputForm) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/appoint-agent")
          }
        }

      }

      "Continue with Agent" must {

        "respond with redirect" in {
          submitWithAuthorisedAgent {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/business-customer/agent/ATED")
          }
        }

      }

      "Cancel link" must {

        "redirect to the start-subscription url" in {
          getWithAuthorisedUser {
            result =>
              val document = Jsoup.parse(contentAsString(result))
          }
        }

      }

      "register" must {

        "respond with BadRequest, if nothing was selected" in {
          val inputForm = Seq(("", ""))
          registerWithAuthorisedUser(inputForm) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("appointAgent-error").text() must be("There is a problem with the appoint an agent question")
            document.getElementById("appointAgent-error-0").text() must be("You must answer the appoint an agent question")
            status(result) must be(BAD_REQUEST)
          }
        }

        "respond with redirect, if no is selected" in {
          val inputForm = Seq(("appointAgent", "false"))
          registerWithAuthorisedUser(inputForm) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/ATED")
          }
        }

      }

      "register with Agent" must {
        "respond with redirect" in {
          registerWithAuthorisedAgent {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/business-customer/agent/ATED")
          }
        }
      }

      "register with Agent Assistant" must {
        "respond with redirect to unauthorised" in {
          registerWithAuthorisedAgentAssistant {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated-subscription/unauthorised-assistant")
          }
        }
      }

    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgentThroughUserLink(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgentAssistant(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgentAssistant(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def subscribeAgentWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithAuthorisedUser(inputForm: Seq[(String, String)])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputForm: _*))

    test(result)
  }

  def submitWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def appointWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.appoint.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def registerWithAuthorisedUser(inputForm: Seq[(String, String)])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestSubscriptionController.register.apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputForm: _*))

    test(result)
  }

  def registerWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestSubscriptionController.register.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def registerWithAuthorisedAgentAssistant(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgentAssistant(userId, mockAuthConnector)
    val result = TestSubscriptionController.register.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

}
