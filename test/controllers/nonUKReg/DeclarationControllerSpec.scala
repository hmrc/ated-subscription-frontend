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

package controllers.nonUKReg

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import models.{EnrolResponse, SubscribeSuccessResponse}
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
import services.{MandateService, RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

class DeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "DeclarationController" must {
    "view" must {
      "return non-uk client registration declaration page" in {
        viewWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration")
          document.getElementById("declaration-header").text must be("Declaration")
          document.getElementById("declaration-lede").text must be("Before you can add a client you must have an ATED 1 in place. This allows HMRC to exchange and disclose information about your client with your agency and to deal with them on matters relating to the Annual Tax on Enveloped Dwellings (ATED) and ATED related Capital Gains Tax (CGT) only. Submission of form 64-8 ‘Authorising your agent’ to HMRC for other tax matters does not cover ATED or ATED related CGT.")
          document.getElementById("subtitle").text must be("Add a client")
          document.getElementById("i-confirm-heading").text must be("I confirm that:")
          document.getElementById("i-confirm-text").text must be("my client has nominated me as an agent to act on their behalf in respect of Annual Tax on Enveloped Dwellings and and that the information I have given is correct and complete.")
          document.getElementById("submit").text must be("Confirm and register")
        }
      }
    }

    "submit" must {
      "for valid form, redirect to confirmation page" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitWithAuthorisedUser(fakeRequest, ated = Some("atedRefNum")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/ated-subscription/agent/confirmation"))
        }
      }

      "for valid form, but API4 call fail, so no atedRefNum is returned in response, throw exception" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitWithAuthorisedUser(fakeRequest, ated = None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("ated reference number not found")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockRegisterUserService = mock[RegisterUserService]
  val mockMandateService = mock[MandateService]

  object TestDeclarationController extends DeclarationController {
    override val authConnector = mockAuthConnector
    override val registerUserService = mockRegisterUserService
    override val mandateService = mockMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegisterUserService)
    reset(mockMandateService)
  }

  def viewWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestDeclarationController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def succResp(ated: Option[String] = None) = SubscribeSuccessResponse(None, ated, Some("formBundleNo"))
  val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))

  def submitWithAuthorisedUser(request: FakeRequest[AnyContentAsFormUrlEncoded], ated: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockRegisterUserService.subscribeAted(Matchers.eq(true))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(succResp(ated), HttpResponse(OK, Some(enrolResp))))
    when(mockMandateService.createMandateForNonUK(Matchers.eq("atedRefNum"))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(CREATED)))
    val result = TestDeclarationController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
