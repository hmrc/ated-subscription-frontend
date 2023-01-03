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

package controllers.auth

import config.ApplicationConfig
import models.AtedSubscriptionAuthData
import play.api.test.Helpers._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtedSubscriptionAuthHelpersSpec extends PlaySpec with AtedSubscriptionAuthHelpers with AuthFunctionality with
  MockitoSugar with GuiceOneAppPerSuite {

  override val authConnector: AuthConnector = mock[AuthConnector]
  override implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
  val func: AtedSubscriptionAuthData => Future[Result] = (_: AtedSubscriptionAuthData) => myFuture

  implicit val fq: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = mock[Messages]

  def buildCreds(credRole: Option[CredentialRole], affinityGroup: Option[AffinityGroup],
                 enrolments: Set[Enrolment]): AtedSubscriptionAuthData = AtedSubscriptionAuthData(
      credRole,
      affinityGroup,
      None,
      Some("credId"),
      Some("asdf"),
      Some("gid"),
      Enrolments(enrolments)
    )

  type RetrievalType = Option[CredentialRole] ~
    Option[AffinityGroup] ~
    Enrolments ~
    Option[String] ~
    Option[Credentials] ~
    Option[String]

  def buildRetrieval(atedSubscriptionAuthData: AtedSubscriptionAuthData): RetrievalType = {
    new ~(
      new ~(
        new ~(
          new ~(
            new ~(
              atedSubscriptionAuthData.credentialRole,
              atedSubscriptionAuthData.affinityGroup
            ),
            atedSubscriptionAuthData.enrolments
          ),
          atedSubscriptionAuthData.agentCode
        ),
        Some(Credentials("mockProvi", "type"))
      ),
      atedSubscriptionAuthData.groupIdentifier
    )
  }

  "agentAction" must {
    "redirect to the unauthorised agent assistant page" when {
      "the user has an agent affinity group" when {
        "the agent has an assistant cred role" when {

          "the agent has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant), Some(AffinityGroup.Agent), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-agent")
          }
        }

        "the agent has a User cred role" when {

          "the agent has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(User), Some(AffinityGroup.Agent), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val response = agentAction(func)
            contentAsString(response) shouldBe "test"

          }

          "the agent has an agent enrolment" in {

            val atedSubscriptionAuthData = buildCreds(Some(User), Some(AffinityGroup.Agent),
              Set(Enrolment(agentEnrolment)))

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val response = agentAction(func)
            contentAsString(response) shouldBe "test"

          }
        }
      }
    }

    "redirect to the unauthorised page" when {
      "the user has an agent affinity group" when {
        "the agent has no cred role" when {

          "the agent has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(None, Some(AffinityGroup.Agent), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised")
          }
        }
      }
    }

    "redirect to the unauthorised organisation assistant page" when {
      "the user has an organisation affinity group" when {
        "the organisation has an assistant cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant), Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-org")
          }

          "the organisation has ated enrolment" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant),
              Some(AffinityGroup.Organisation), Set(Enrolment(atedEnrolment)))

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-org")
          }
        }

        "the organisation has a User cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(User), Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised")
          }
        }
      }
    }

    "redirect to the unauthorised page" when {
      "the user has an organisation affinity group" when {
        "the organisation has no cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(None, Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = agentAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised")
          }
        }
      }
    }


  }

  "clientAction" must {
    "redirect to the unauthorised agent assistant page" when {
      "the user has an agent affinity group" when {
        "the agent has an assistant cred role" when {

          "the agent has an agent enrolment" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant),
              Some(AffinityGroup.Agent), Set(Enrolment(agentEnrolment)))

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = clientAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-agent")
          }
        }
      }
    }

    "redirect to the unauthorised page" when {
      "the user has an agent affinity group" when {
        "the agent has a User cred role" when {

          "the agent has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(User), Some(AffinityGroup.Agent), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            when(appConfig.serviceRedirectUrl(ArgumentMatchers.any()))
              .thenReturn("businessCustUrl")

            val res: Future[Result] = clientAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("businessCustUrl")

          }
        }
      }
    }

    "redirect to the unauthorised page" when {
      "the user has an agent affinity group" when {
        "the agent has no cred role" when {

          "the agent has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(None, Some(AffinityGroup.Agent), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val response = clientAction(func)
            contentAsString(response) shouldBe "test"
          }
        }
      }
    }

    "redirect to the unauthorised organisation assistant page" when {
      "the user has an org affinity group" when {
        "the organisation has an assistant cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant), Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = clientAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-org")
          }

          "the organisation has ated enrolment" in {

            val atedSubscriptionAuthData = buildCreds(Some(Assistant),
              Some(AffinityGroup.Organisation), Set(Enrolment(atedEnrolment)))

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val res: Future[Result] = clientAction(func)
            status(res) shouldBe 303
            redirectLocation(res) shouldBe Some("/ated-subscription/unauthorised-assistant-org")
          }
        }

        "the organisation has a User cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(Some(User), Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val response = clientAction(func)
            contentAsString(response) shouldBe "test"
          }
        }
      }
    }

    "redirect to the unauthorised page" when {
      "the user has an organisation affinity group" when {
        "the organisation has no cred role" when {

          "the organisation has no enrolments" in {

            val atedSubscriptionAuthData = buildCreds(None, Some(AffinityGroup.Organisation), Set())

            when(authConnector.authorise[RetrievalType](ArgumentMatchers.any(),
              ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
              .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

            val response = clientAction(func)
            contentAsString(response) shouldBe "test"
          }
        }
      }
    }
  }

}
