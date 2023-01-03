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

package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}

class EtmpModelsSpec extends PlaySpec {

  val etmpAddressDetailsModel: EtmpAddressDetails =
      EtmpAddressDetails(addressType = "Principal Place Of Business", addressLine1 = "AL1", addressLine2 = "AL2", None, None, None, "GB")

  val etmpAddressDetailsJson: JsValue = Json.parse(
    """{
      | "addressType":"Principal Place Of Business",
      | "addressLine1":"AL1",
      | "addressLine2":"AL2",
      | "postalCode":null,
      | "countryCode":"GB"
      |}""".stripMargin)

  "EtmpAddressDetails" should {
    "read correctly into a model" in {
      etmpAddressDetailsJson.as[EtmpAddressDetails] mustBe etmpAddressDetailsModel
    }

    "write correctly to json when no postalcode has been provided" in {
      Json.toJson(etmpAddressDetailsModel) mustBe etmpAddressDetailsJson
    }
  }

}
