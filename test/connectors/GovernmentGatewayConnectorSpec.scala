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

package connectors

import java.util.UUID

import builders.{AuthBuilder, TestAudit}
import metrics.Metrics
import models.{EnrolRequest, EnrolResponse, Identifier}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

import scala.concurrent.Future
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId


class GovernmentGatewayConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp = mock[MockedVerbs]

  object TestGovernmentGatewayConnector extends GovernmentGatewayConnector {
    override val http: CoreGet with CorePost = mockWSHttp
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"

    override def metrics = Metrics
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "GovernmentGatewayConnector" must {

    val request = EnrolRequest(portalId = "ATED", serviceName = "ATED", friendlyName = "Main Enrolment", knownFacts = Seq("ATED-123"))
    val response = Json.toJson(EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No"))))
    val successfulSubscribeJson = HttpResponse(OK, Some(response))
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val user = AuthBuilder.createUserAuthContext("User-Id", "name")

    "use correct metrics" in {
      GovernmentGatewayConnector.metrics must be(Metrics)
    }

    "enrol user" must {
      "works for a user" in {

        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).
          thenReturn(Future.successful(successfulSubscribeJson))

        val result = TestGovernmentGatewayConnector.enrol(request)
        val enrolResponse = await(result)
        enrolResponse.json must be(response)
      }

      "return status as BAD_REQUEST, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[BadRequestException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status anything else, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = TestGovernmentGatewayConnector.enrol(request)
        val thrown = the[InternalServerException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }
  }
}
