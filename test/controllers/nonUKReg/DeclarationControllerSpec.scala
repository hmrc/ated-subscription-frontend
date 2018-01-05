/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.AgentClientMandateFrontendConnector
import models.{EnrolResponse, OldMandateReference, SubscribeSuccessResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{MandateService, NewRegisterUserService, RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class DeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "DeclarationController" must {
    "view" must {
      "return non-uk client registration declaration page" in {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration - GOV.UK")
          document.getElementById("declaration-header").text must be("Declaration")
          document.getElementById("subtitle").text must be("This section is: Add a client")
          document.getElementById("i-confirm-text").text must be("I confirm that my client has nominated me as an agent to act on their behalf in respect of Annual Tax on Enveloped Dwellings and that the information I have given is correct and complete.")
          document.getElementById("submit").text must be("Confirm and register")
        }
      }
    }

    "submit" must {
      "for valid form, redirect to confirmation page" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        val oldMandateRef = Some(OldMandateReference("mandateId", "atedRefNum"))
        submitWithAuthorisedUser(fakeRequest, ated = Some("atedRefNum"), oldMandateRef) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated-subscription/agent/confirmation"))
        }
      }

      "for valid form, but API4 call fail, so no atedRefNum is returned in response, throw exception" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        val oldMandateRef = Some(OldMandateReference("mandateId", "atedRefNum"))
        submitWithAuthorisedUser(fakeRequest, ated = None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("ated reference number not found")
        }
      }

      "for valid form, but NO previous mandate details - redirect to confirmation page" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitWithAuthorisedUser(fakeRequest, ated = Some("atedRefNum")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated-subscription/agent/confirmation"))
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockRegisterUserService = mock[RegisterUserService]
  val mockMandateService = mock[MandateService]
  val mockAgentClientFrontendMandateConnector = mock[AgentClientMandateFrontendConnector]
  val mockRegisterEmacUserService = mock[NewRegisterUserService]

  object TestDeclarationController extends DeclarationController {
    override val authConnector = mockAuthConnector
    override val registerUserService = mockRegisterUserService
    override val mandateService = mockMandateService
    override val agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector = mockAgentClientFrontendMandateConnector
    override val registerEmacUserService = mockRegisterEmacUserService
    val isEmacFeatureToggle: Boolean = true
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockMandateService)
    reset(mockAgentClientFrontendMandateConnector)
  }

  def viewWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestDeclarationController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def succResp(ated: Option[String] = None) = SubscribeSuccessResponse(None, ated, Some("formBundleNo"))
  val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))

  def submitWithAuthorisedUser(request: FakeRequest[AnyContentAsFormUrlEncoded], ated: Option[String] = None,
                               oldMandateRef: Option[OldMandateReference] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockAgentClientFrontendMandateConnector.getOldMandateDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(oldMandateRef))
    when(mockRegisterEmacUserService.subscribeAted(Matchers.eq(true))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(succResp(ated), HttpResponse(OK, Some(enrolResp))))
    when(mockMandateService.createMandateForNonUK(Matchers.eq("atedRefNum"))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
    when(mockMandateService.updateMandateForNonUK(Matchers.eq("atedRefNum"), Matchers.eq("mandateId"))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
    val result = TestDeclarationController.submit.apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
