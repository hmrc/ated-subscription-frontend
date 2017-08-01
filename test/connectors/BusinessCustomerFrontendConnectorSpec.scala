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
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter

import scala.concurrent.Future


class BusinessCustomerFrontendConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "BusinessCustomerFrontendConnector" must {
    "getReviewDetails" in {
      val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")
      val reviewDetails = ReviewDetails(businessName = "ACME",
        businessType = Some("corporate body"),
        businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
        sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(reviewDetails)))))

      val response = await(TestBusinessCustomerFrontendConnector.getReviewDetails)
      response.status must be(OK)
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
  implicit val request: Request[_] = FakeRequest(GET, "")

  val mockWSHttp = mock[WSHttp]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestBusinessCustomerFrontendConnector extends BusinessCustomerFrontendConnector {
    override val http: HttpGet = mockWSHttp
    override def crypto: (String) => String = SessionCookieCryptoFilter.encrypt _
  }
}
