/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.Auditable
import builders.AuthBuilder
import com.codahale.metrics.MetricRegistry
import metrics.Metrics
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId


class TaxEnrolmentsConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  val mockAuditable: Auditable = mock[Auditable]

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
    reset(mockAuditable)
  }

  val testTaxEnrolmentsConnector = new TaxEnrolmentsConnector(mockAppConfig, mockAuditable, mockWSHttp, new Metrics)

  "TaxEnrolmentsConnector" must {

    val request = RequestEMACPayload(userId = "user-id", friendlyName = "friendlyName", `type` = "type", verifiers = List(Verifier(key = "key", value = "value")))
    val groupId = "groupId"
    val atedRefNo = "atedRefNo"
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val user: AtedSubscriptionAuthData = AuthBuilder.createUserAuthContext("User-Id", "name")

    "enrol user" must {
      "works for a user" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.successful(HttpResponse(CREATED)))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }


      "return CONFLICT when retrieving a bad request exception from tax enrolments" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.failed(new BadRequestException("Error")))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(CONFLICT)
      }

      "return 301 when retrieving a Upstream response of 301 from tax enrolments" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.failed( new Upstream5xxResponse("", 301,301)))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(301)
      }

      "return Internal server error when retrieving a Internal Server Exception from tax enrolments" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.failed(new InternalServerException("Error")))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(INTERNAL_SERVER_ERROR)
      }

      "return status anything else, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(INTERNAL_SERVER_ERROR)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }
}
