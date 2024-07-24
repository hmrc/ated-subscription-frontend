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

package connectors

import audit.Auditable
import metrics.Metrics
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentsConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  class Test extends ConnectorMocks {
    val mockAuditable: Auditable = mock[Auditable]
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    when(mockAppConfig.serviceUrlTaxEnrol).thenReturn("http://localhost:9020/")
    val testTaxEnrolmentsConnector = new TaxEnrolmentsConnector(mockAppConfig, mockAuditable, mockHttpClient, new Metrics)
  }

  "TaxEnrolmentsConnector" must {

    val request = RequestEMACPayload(userId = "user-id", friendlyName = "friendlyName", `type` = "type",
      verifiers = List(Verifier(key = "key", value = "value")))
    val groupId = "groupId"
    val atedRefNo = "atedRefNo"
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")

    "enrol user" must {
      "works for a user" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(CREATED, "")))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }

      "return CONFLICT when retrieving a bad request exception from tax enrolments" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.failed(new ConflictException("Error")))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(CONFLICT)
      }

      "return Internal server error when retrieving a Internal Server Exception from tax enrolments" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.failed(new InternalServerException("Error")))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(INTERNAL_SERVER_ERROR)
      }

      "throw a RuntimeException when receiving a BAD_REQUEST from tax enrolments" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.failed(new BadRequestException("INVALID_JSON")))
        intercept[RuntimeException] {
          await(testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo))
        }
      }

      "return status anything else, for bad data sent for enrol" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString())))
        val result = testTaxEnrolmentsConnector.enrol(request, groupId, atedRefNo)
        val enrolResponse = await(result)
        enrolResponse.status must be(INTERNAL_SERVER_ERROR)
        verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
      }
    }
  }
}
