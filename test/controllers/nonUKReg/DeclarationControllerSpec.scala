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

package controllers.nonUKReg

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import connectors.AgentClientMandateFrontendConnector
import models.{EnrolResponse, OldMandateReference, SubscribeSuccessResponse}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.MandateService
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http.HttpResponse
import views.html.nonUKReg.declaration

import scala.concurrent.Future

class DeclarationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockMandateService: MandateService = mock[MandateService]
  val mockAgentClientFrontendMandateConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val injectedViewInstance: declaration = app.injector.instanceOf[views.html.nonUKReg.declaration]

  val testDeclarationControllerWithEMAC: DeclarationController = new DeclarationController(mockMCC,
                                                                                           mockRegisterUserService,
                                                                                           mockMandateService,
                                                                                           mockAgentClientFrontendMandateConnector,
                                                                                           mockAuthConnector,
                                                                                           injectedViewInstance,
                                                                                           mockAppConfig)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockMandateService)
    reset(mockAgentClientFrontendMandateConnector)
  }

  def viewWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = testDeclarationControllerWithEMAC.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def succResp(ated: Option[String] = None): SubscribeSuccessResponse = SubscribeSuccessResponse(None, ated, Some("formBundleNo"))
  val enrolResp: JsValue = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))

  def submitWithAuthorisedUserForEmac(request: FakeRequest[AnyContentAsFormUrlEncoded], ated: Option[String] = None,
                                      oldMandateRef: Option[OldMandateReference] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockAgentClientFrontendMandateConnector.getOldMandateDetails(any(), any()))
      .thenReturn(Future.successful(oldMandateRef))
    when(mockRegisterUserService.subscribeAted(eqTo(true))(any(), any(), any(), any()))
      .thenReturn(Future.successful(succResp(ated)))
    when(mockMandateService.createMandateForNonUK(eqTo("atedRefNum"))(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(CREATED, "")))
    when(mockMandateService.updateMandateForNonUK(eqTo("atedRefNum"), eqTo("mandateId"))
    (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse.apply(CREATED, "")))
    val result = testDeclarationControllerWithEMAC.submit.apply(SessionBuilder.updateRequestWithSession(request, userId))
    test(result)
  }

  "DeclarationController" must {
    "view" must {
      "return non-uk client registration declaration page" in {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration - GOV.UK")
          document.getElementById("declaration-header").text must include("Declaration")
          document.getElementById("subtitle").text must be("This section is: Add a client")
          document.getElementById("i-confirm-text").text must be("I confirm that my client has nominated me as an agent" +
                                                                      " to act on their behalf in respect of Annual Tax on" +
                                                                      " Enveloped Dwellings and that the information I have " +
                                                                      "given is correct and complete.")
          document.getElementById("submit").text must be("Confirm and register")
        }
      }
    }

    "submit" must {

      "enrolling to EMAC" when {

        "for valid form, redirect to confirmation page" in {
          val fakeRequest = FakeRequest().withFormUrlEncodedBody()
          val oldMandateRef = Some(OldMandateReference("mandateId", "atedRefNum"))
          submitWithAuthorisedUserForEmac(fakeRequest, ated = Some("atedRefNum"), oldMandateRef) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated-subscription/agent/confirmation"))
          }
        }

        "for valid form, but API4 call fail, so no atedRefNum is returned in response, throw exception" in {
          val fakeRequest = FakeRequest().withFormUrlEncodedBody()
          submitWithAuthorisedUserForEmac(fakeRequest, ated = None) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("ated reference number not found")
          }
        }

        "for valid form, but NO previous mandate details - redirect to confirmation page" in {
          val fakeRequest = FakeRequest().withFormUrlEncodedBody()
          submitWithAuthorisedUserForEmac(fakeRequest, ated = Some("atedRefNum")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated-subscription/agent/confirmation"))
          }
        }
      }
    }
  }

}
