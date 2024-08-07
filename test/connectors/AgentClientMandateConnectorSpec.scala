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

import builders.AuthBuilder
import config.ApplicationConfig
import models.{AtedSubscriptionAuthData, NonUKClientDto}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientMandateConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val dto: NonUKClientDto = NonUKClientDto("safeid", "atedRefNum", "ated", "aa@mail.com", "arn", "bb@mail.com", "client display name")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")

  class Test extends ConnectorMocks {
    val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
    val testAgentClientMandateConnector: AgentClientMandateConnector = new AgentClientMandateConnector(mockAppConfig, mockHttpClient) {
      override lazy val serviceURL: String = "http://localhost:9020/test"
    }
  }

  "AgentClientMandateConnector" must {

    "createMandateForNonUK" must {

      "return response, if status is CREATED" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(CREATED, Some(Json.parse("""{ "reason": "wrong data" }""")).toString)))
        val result = await(testAgentClientMandateConnector.createMandateForNonUK(dto))
        result.status must be(CREATED)
      }

      "throw exception, if response status is anything else" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, null)))
        val thrown = the[InternalServerException] thrownBy await(testAgentClientMandateConnector.createMandateForNonUK(dto))
        thrown.getMessage must be(null)
      }

    }

    "updateMandateForNonUK" must {

      "return response, if status is CREATED" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(CREATED, Some(Json.parse("""{ "reason": "wrong data" }""")).toString)))
        val result = await(testAgentClientMandateConnector.updateMandateForNonUK(dto))
        result.status must be(CREATED)
      }

      "throw exception, if response status is anything else" in new Test {
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, null)))
        val thrown = the[InternalServerException] thrownBy await(testAgentClientMandateConnector.updateMandateForNonUK(dto))
        thrown.getMessage must be(null)
      }

    }

  }
}
