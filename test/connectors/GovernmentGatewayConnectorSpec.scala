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

package connectors

import java.util.UUID

import audit.Auditable
import builders.AuthBuilder
import com.codahale.metrics.{MetricRegistry, Timer}
import config.AtedSubscriptionFrontendAuditConnector
import metrics.Metrics
import models.{AtedSubscriptionAuthData, EnrolRequest, EnrolResponse, Identifier}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class GovernmentGatewayConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockAuditable: Auditable = mock[Auditable]

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
    reset(mockAuditable)
  }

  val testGovernmentGatewayConnector: GovernmentGatewayConnector = new GovernmentGatewayConnector(mockAppConfig, mockAuditable, mockWSHttp, new Metrics){
    override lazy val serviceURL: String = "test"
  }

  "GovernmentGatewayConnector" must {

    val request = EnrolRequest(portalId = "ATED", serviceName = "ATED", friendlyName = "Main Enrolment", knownFacts = Seq("ATED-123"))
    val response = Json.toJson(EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No"))))
    val successfulSubscribeJson = HttpResponse(OK, Some(response))
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val user: AtedSubscriptionAuthData = AuthBuilder.createUserAuthContext("User-Id", "name")


    "enrol user" must {
      "works for a user" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.successful(successfulSubscribeJson))

        val result = testGovernmentGatewayConnector.enrol(request)
        val enrolResponse = await(result)
        enrolResponse.json must be(response)
      }

      "return status as BAD_REQUEST, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
        val result = testGovernmentGatewayConnector.enrol(request)
        val thrown = the[BadRequestException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }
      "return status anything else, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = testGovernmentGatewayConnector.enrol(request)
        val thrown = the[InternalServerException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }
}
