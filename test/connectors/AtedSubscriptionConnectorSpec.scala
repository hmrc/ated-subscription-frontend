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

import builders.AuthBuilder
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._

import scala.concurrent.Future

class AtedSubscriptionConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp = mock[MockedVerbs]

  override def beforeEach = {
    reset(mockWSHttp)
  }

  object TestAtedSubscriptionConnector extends AtedSubscriptionConnector {
    override val http: CoreGet with CorePost = mockWSHttp
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val subscribeSuccessResponse = SubscribeSuccessResponse(processingDate = Some("2001-12-17T09:30:47Z"),
    atedRefNumber = Some("ABCDEabcde12345"), formBundleNumber = Some("123456789012345"))
  val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
  val subscribeSuccessResponseJson = Json.toJson(subscribeSuccessResponse)
  val subscribeData = SubscribeData(safeId = "EX0012345678909", acknowledgementReference = "1234567890",
    address = List(EtmpCorrespondence(name1 = "Joe", name2 = "Bloggs",
      addressDetails = EtmpAddressDetails("Correspondence", "line1", "line2", None, None, None, "GB"),
      contactDetails = EtmpContactDetails(Some("01234567890"), None, None, Some("a@b.c")))), emailConsent = true, utr = "1234567890", isNonUKClientRegisteredByAgent = false, knownFactPostcode=Some("AA1 1AA"))
  val subscribeDataJson = Json.toJson(subscribeData)

  "AtedSubscriptionConnector" must {
    "subscribeAted" must {
      "return status as OK, for successful subscription" in {
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(subscribeSuccessResponseJson))))
        val result = TestAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        await(result) must be(subscribeSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status as BAD_REQUEST, for bad data sent for subscription" in {
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(subscribeFailureResponseJson))))
        val result = TestAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        val thrown = the[BadRequestException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
      "return status anything else, for bad data sent for subscription" in {
        implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = TestAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        val thrown = the[InternalServerException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

  }

}
