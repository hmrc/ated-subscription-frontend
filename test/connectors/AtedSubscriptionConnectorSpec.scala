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
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import testHelpers.AtedTestHelper
import uk.gov.hmrc.http._
import utils.TestJson

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtedSubscriptionConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with
  BeforeAndAfterEach with AtedTestHelper with TestJson {

  override def beforeEach: Unit = {
    reset(mockAppConfig)
    reset(mockWSHttp)
  }

  val testAtedSubscriptionConnector: AtedSubscriptionConnector = new AtedSubscriptionConnector(mockAppConfig, mockWSHttp) {
    override lazy val serviceURL: String = "test"
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

  val subscribeSuccessResponse = SubscribeSuccessResponse(processingDate = Some("2001-12-17T09:30:47Z"),
    atedRefNumber = Some("ABCDEabcde12345"), formBundleNumber = Some("123456789012345"))
  val subscribeFailureResponseJson: JsValue = Json.parse( """{"reason" : "Error happened"}""")
  val subscribeSuccessResponseJson: JsValue = Json.toJson(subscribeSuccessResponse)
  val subscribeData = AtedSubscriptionRequest(safeId = "EX0012345678909", acknowledgementReference = "1234567890",
    address = List(EtmpCorrespondence(name1 = "Joe", name2 = "Bloggs",
      addressDetails = EtmpAddressDetails("Correspondence", "line1", "line2", None, None, None, "GB"),
      contactDetails = EtmpContactDetails(Some("01234567890"), None, None, Some("a@b.c")))), emailConsent = true,
    businessType = "Corporate Body", utr = Some("1234567890"), isNonUKClientRegisteredByAgent = false, knownFactPostcode = Some("AA1 1AA"))
  val subscribeDataJson: JsValue = Json.toJson(subscribeData)

  "AtedSubscriptionConnector" must {
    "subscribeAted" must {

      "return status as OK, for successful subscription" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, subscribeSuccessResponseJson.toString())))
        val result = testAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        await(result) must be(subscribeSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "return status as BAD_REQUEST, for bad data sent for subscription" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, subscribeFailureResponseJson.toString())))
        val result = testAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        val thrown = the[BadRequestException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "return status anything else, for bad data sent for subscription" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString())))
        val result = testAtedSubscriptionConnector.subscribeAted(subscribeDataJson)
        val thrown = the[InternalServerException] thrownBy await(result)
        Json.parse(thrown.getMessage) must be(subscribeFailureResponseJson)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }

    "checkEtmpBusinessPartnerExists" must {
      "return a self heal subscription when an OK has been received" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.obj("regimeRefNumber" -> "test").toString())))
        val result = testAtedSubscriptionConnector.checkEtmpBusinessPartnerExists(etmpCheckOrganisation)
        await(result) must be(Some(SelfHealSubscriptionResponse("test")))
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "return a None for any other status received" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, Json.obj().toString())))
        val result = testAtedSubscriptionConnector.checkEtmpBusinessPartnerExists(etmpCheckOrganisation)
        await(result) must be(None)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }
}
