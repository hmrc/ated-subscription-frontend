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

import builders.AuthBuilder
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class BusinessCustomerFrontendConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with AtedTestHelper {

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
  implicit val request: Request[_] = FakeRequest(GET, "")

  val testBusinessCustomerFrontendConnector: BusinessCustomerFrontendConnector = new BusinessCustomerFrontendConnector(mockAppConfig, mockWSHttp) {
    override val serviceUrl: String = "test"
  }

  "BusinessCustomerFrontendConnector" must {
    "getBusinessCustomerDetails" in {
      val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")
      val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
        businessType = Some("corporate body"),
        businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
        sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
      when(mockWSHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(reviewDetails)))))

      val response = await(testBusinessCustomerFrontendConnector.getBusinessCustomerDetails)
      response.status must be(OK)
    }

    "crypto" in {
      val testString = "I 5! stay 6? the 7π same"
      testBusinessCustomerFrontendConnector.crypto(testString) must be(testString)
    }
  }


}
