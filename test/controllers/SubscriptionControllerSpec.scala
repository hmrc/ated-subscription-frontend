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
import views.html.{agentSubscription, appointAgent, beforeRegisterAgent, beforeRegisteringForATED, subscription}
import scala.concurrent.Future

class SubscriptionControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val injectedViewInstanceSubscription: subscription = app.injector.instanceOf[views.html.subscription]
  val injectedViewInstanceAppointAgent: appointAgent = app.injector.instanceOf[views.html.appointAgent]
  val injectedViewInstanceAgentSubscription: agentSubscription = app.injector.instanceOf[views.html.agentSubscription]
  val injectedViewInstanceBeforeRegisterAgent: beforeRegisterAgent = app.injector.instanceOf[views.html.beforeRegisterAgent]
  val injectedViewInstanceBeforeRegisteringForATED: beforeRegisteringForATED = app.injector.instanceOf[views.html.beforeRegisteringForATED]
  val testSubscriptionController = new SubscriptionController(mockMCC,
    mockAuthConnector,
    injectedViewInstanceSubscription,
    injectedViewInstanceAppointAgent,
    injectedViewInstanceAgentSubscription,
    injectedViewInstanceBeforeRegisterAgent,
    injectedViewInstanceBeforeRegisteringForATED,
    mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "SubscriptionController" must {

    "Start subscription" must {
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

              document.title() must be("Are you an agent acting for a client? - GOV.UK")
              document.getElementById("client-startpage-header").text() must include("Are you an agent acting for a client?")
              document.getElementById("submit").text() must be("Continue")
              assert(document.select(".govuk-header__service-name").attr("href") === "/ated-subscription/start-subscription")


          }
        }

        "return the start subscription page for an Organisation assistant" in {
          getWithAuthorisedOrgAssistant {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.title() must be("Are you an agent acting for a client? - GOV.UK")
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
              redirectLocation(result) must be(Some("/ated-subscription/unauthorised-assistant-agent"))
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
              document.title() must be("Set up your agency for the new ATED online service - GOV.UK")
              document.getElementById("subtitle").text() must be("This section is: ATED agency set up")
              document.getElementById("agent-startpage-header").text() must include("Set up your agency for the new ATED online service")
              document.getElementById("lede-paragraph").text() must include("Before you can submit ATED returns on behalf of your clients you must set up your agency")
              document.getElementById("agent-startpage-text1").text() must be("You must enter your agency’s registered name and Unique Taxpayer Reference (UTR).")
              document.getElementById("agent-startpage-text2").text() must be("After setting up your details, you can add your clients.")
              document.getElementById("submit").text() must be("Set up your agency")
              assert(document.select(".govuk-header__service-name").attr("href") === "/ated-subscription/start-subscription")
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
            document.getElementById("client-startpage-header").text() must include("Do you want to appoint an agent to act for you?")
            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated-subscription/start-subscription")
          }
        }
      }

      "beforeRegisterGuidance" must {

        "return BadRequest if no option selected" in {
          val input = Seq("" -> "")
          beforeRegisterGuidanceWithAuthorisedUser(input) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }

        "render the agent guidance page when Yes selected" in {
          val input = Seq("appointAgent" -> "true")
          beforeRegisterGuidanceWithAuthorisedUser(input) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/before-registering-agent")
          }
        }

        "render the client guidance page when No selected" in {
          val input = Seq("appointAgent" -> "false")
          beforeRegisterGuidanceWithAuthorisedUser(input) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/before-registering-ated")
          }
        }

        "view the before registering agent page" in {
          showBeforeRegisteringAgentPageWithAuthorisedUser() {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Before registering your ATED agent - GOV.UK")
              document.getElementById("client-appoint-subheader").text() must be("This section is: ATED registration")
              document.getElementById("before-register-agent-header").text() must be("This section is: ATED registration")
              document.getElementById("submit").text() must be("Continue")
              assert(document.select(".govuk-header__service-name").attr("href") === "/ated-subscription/start-subscription")
          }
        }

        "view the before registering for ATED page" in {
          showBeforeRegisteringATEDPageWithAuthorisedUser() {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Before registering for ATED - GOV.UK")
              document.getElementById("client-appoint-subheader").text() must be("This section is: ATED registration")
              document.getElementById("client-startpage-header").text() must be("This section is: ATED registration")
              document.getElementById("submit").text() must be("Continue")
              assert(document.select(".govuk-header__service-name").attr("href") === "/ated-subscription/start-subscription")
          }
        }
      }

      "Continue" must {

        "respond with BadRequest, if yes was selected" in {
          val inputForm = Seq(("isAgent", "true"))
          submitWithAuthorisedUser(inputForm) { result =>
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("isAgent-error").text() must be("Error: You must sign in with your agent details if you are an agent")
            document.getElementById("hidden-isAnAgent").text() must include("If you are an agent acting for a client you need to sign in using your agent Government Gateway details.")
            document.getElementById("hidden-isAnAgent").getElementsByTag("a").first().attr("href") must be("http://localhost:9025/gg/sign-in?continue=http://localhost:9933/ated-subscription/start-subscription")
            status(result) must be(BAD_REQUEST)
          }
        }

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


    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgentThroughUserLink(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedAgent(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedAgentAssistant(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgentAssistant(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedOrgAssistant(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedOrgAssistant(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribe.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def subscribeAgentWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.subscribeAgent.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithAuthorisedUser(inputForm: Seq[(String, String)])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSession(userId).withFormUrlEncodedBody(inputForm: _*))

    test(result)
  }

  def submitWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = testSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any): Unit = {
    AuthBuilder.mockUnAuthorisedUserNotLogged(mockAuthConnector)
    val result = testSubscriptionController.continue.apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def appointWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.appoint.apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def beforeRegisterGuidanceWithAuthorisedUser(inputForm: Seq[(String, String)])
                                              (test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.beforeRegisterGuidance
      .apply(SessionBuilder.buildRequestWithSession(userId)
        .withFormUrlEncodedBody(inputForm: _*))
    test(result)
  }

  def showBeforeRegisteringAgentPageWithAuthorisedUser()(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.showBeforeRegisteringAgentPage
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def showBeforeRegisteringATEDPageWithAuthorisedUser()(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testSubscriptionController.showBeforeRegisteringATEDPage
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
