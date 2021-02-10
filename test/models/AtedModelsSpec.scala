/*
 * Copyright 2021 HM Revenue & Customs
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

class AtedModelsSpec extends PlaySpec {

  val businessCustomerDetailsModel: BusinessCustomerDetails =
    BusinessCustomerDetails(
      businessName = "Logical Logistics",
      businessType = "Corporate Body",
      businessAddress = Address("22 Gordon Road", "Riverside", Some("Town"), Some("Shropshire"), Some("TF2 8JP"), "United Kingdom"),
      sapNumber = "325435235235",
      safeId = "2343534523",
      agentReferenceNumber = Some("LARN123456"),
      directMatch = true,
      identification = Some(Identification(idNumber = "123123", issuingInstitution = "Companies House", issuingCountryCode = "UK")),
      utr = Some("1111111111"),
      isBusinessDetailsEditable = true)

  def businessCustomerDetailsJson(businessType: String): JsValue = Json.parse(
    s"""{
      | "businessName":"Logical Logistics",
      | "businessType":"$businessType",
      | "businessAddress": {
      |   "line_1":"22 Gordon Road",
      |   "line_2":"Riverside",
      |   "line_3":"Town",
      |   "line_4":"Shropshire",
      |   "postcode":"TF2 8JP",
      |   "country":"United Kingdom"
      | },
      | "sapNumber":"325435235235",
      | "safeId":"2343534523",
      | "agentReferenceNumber":"LARN123456",
      | "directMatch":true,
      | "identification": {
      |   "idNumber":"123123",
      |   "issuingInstitution":"Companies House",
      |   "issuingCountryCode":"UK"
      | },
      | "utr":"1111111111",
      | "isBusinessDetailsEditable":true
      |}""".stripMargin)

  def businessCustomerDetailsJsonNoUtr(businessType: String): JsValue = Json.parse(
    s"""{
       | "businessName":"Logical Logistics",
       | "businessType":"$businessType",
       | "businessAddress": {
       |   "line_1":"22 Gordon Road",
       |   "line_2":"Riverside",
       |   "line_3":"Town",
       |   "line_4":"Shropshire",
       |   "postcode":"TF2 8JP",
       |   "country":"United Kingdom"
       | },
       | "sapNumber":"325435235235",
       | "safeId":"2343534523",
       | "agentReferenceNumber":"LARN123456",
       | "directMatch":true,
       | "identification": {
       |   "idNumber":"123123",
       |   "issuingInstitution":"Companies House",
       |   "issuingCountryCode":"UK"
       | },
       | "isBusinessDetailsEditable":true
       |}""".stripMargin)

  "BusinessCustomerDetails" should {
    "read correctly into a model" in {
      businessCustomerDetailsJson("Corporate Body").as[BusinessCustomerDetails] mustBe businessCustomerDetailsModel
    }

    "read correctly into a model when there is no utr" in {
      businessCustomerDetailsJsonNoUtr("Corporate Body").as[BusinessCustomerDetails] mustBe businessCustomerDetailsModel.copy(utr = None)
    }

    "fail to read to a model if an invalid businessType has been provided" in {
      assertThrows[RuntimeException](businessCustomerDetailsJson("Fake business type").as[BusinessCustomerDetails])
    }

    "write correctly to json all fields have been provided" in {
      Json.toJson(businessCustomerDetailsModel) mustBe businessCustomerDetailsJson("Corporate Body")
    }

    "write correctly to json when there is no utr" in {
      Json.toJson(businessCustomerDetailsModel.copy(utr = None)) mustBe businessCustomerDetailsJsonNoUtr("Corporate Body")
    }
  }
}
