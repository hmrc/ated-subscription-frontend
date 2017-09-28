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

package connectors

import builders.AuthBuilder
import models.NonUKClientDto
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse, InternalServerException }

class AgentClientMandateConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "AgentClientMandateConnector" must {

    "createMandateForNonUK" must {

      "return response, if status is CREATED" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED, Some(Json.parse("""{ "reason": "wrong data" }""")))))
        val result = await(TestAgentClientMandateConnector.createMandateForNonUK(dto))
        result.status must be(CREATED)
      }

      "throw exception, if response status is anything else" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        val thrown = the[InternalServerException] thrownBy await(TestAgentClientMandateConnector.createMandateForNonUK(dto))
        thrown.getMessage must be(null)
      }

    }

    "updateMandateForNonUK" must {

      "return response, if status is CREATED" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED, Some(Json.parse("""{ "reason": "wrong data" }""")))))
        val result = await(TestAgentClientMandateConnector.updateMandateForNonUK(dto))
        result.status must be(CREATED)
      }

      "throw exception, if response status is anything else" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        val thrown = the[InternalServerException] thrownBy await(TestAgentClientMandateConnector.updateMandateForNonUK(dto))
        thrown.getMessage must be(null)
      }

    }

  }

  val dto = NonUKClientDto("safeid", "atedRefNum", "ated", "aa@mail.com", "arn", "bb@mail.com", "client display name")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")

  val mockWSHttp = mock[WSHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestAgentClientMandateConnector extends AgentClientMandateConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

}
