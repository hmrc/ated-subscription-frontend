/*
 * Copyright 2019 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthFunctionalitySpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  class Setup {
    val controllerHarness: AuthFunctionality = new AuthFunctionality {
      override val authConnector: AuthConnector = mockAuthConnector
      override val appConfig: ApplicationConfig = mockAppConfig
    }
  }

  type RetrievalType = Option[CredentialRole] ~
    Option[AffinityGroup] ~
    Enrolments ~
    Option[String] ~
    Option[Credentials]

  def buildRetrieval(atedSubscriptionAuthData: AtedSubscriptionAuthData): RetrievalType = {
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
    )
  }

  "authoriseFor" should {
    "authorise a user" when {
      "they have an ated enrolment" in new Setup {
        val atedSubscriptionAuthData: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
          Some(Assistant),
          Some(AffinityGroup.Agent),
          None,
          Some("asdf"),
          Enrolments(Set(Enrolment(controllerHarness.atedEnrolment)))
        )

        when(mockAuthConnector.authorise[RetrievalType](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))

        val myFuture: Future[Result] = Future.successful(Results.Ok("test"))
        val func: AtedSubscriptionAuthData => Future[Result] = (_: AtedSubscriptionAuthData) => myFuture

        implicit val fq: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        implicit val messages: Messages = mock[Messages]

        val res: Future[Result] = controllerHarness.authoriseFor(func)
        status(res) shouldBe 200
      }
    }
  }

}
