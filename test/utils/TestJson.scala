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

package utils

import models.{EtmpAddressDetails, EtmpContactDetails, EtmpCorrespondence, AtedSubscriptionRequest}
import play.api.libs.json.{JsValue, Json}

trait TestJson {

  val etmpSubscribeDataModel = AtedSubscriptionRequest(
    acknowledgementReference = "XN123434678569",
    safeId = "243567679865",
    emailConsent = false,
    address = List(EtmpCorrespondence(
      name1 = "a first name",
      name2 = "another name",
      addressDetails = EtmpAddressDetails(
        addressType = "Business",
        addressLine1 = "23 Springfield Drive",
        addressLine2 = "Springfield",
        addressLine3 = None,
        addressLine4 = None,
        postalCode = None,
       countryCode = "US"
      ),
      contactDetails = EtmpContactDetails(
        phoneNumber = Some("0121-345-234"),
        mobileNumber = Some("07124343234"),
        faxNumber = None,
        emailAddress = None
      )
    )),
    businessType = "Corporate Body",
    utr = Some("123456789"),
    isNonUKClientRegisteredByAgent = true,
    knownFactPostcode = None
  )

  val etmpSubscribeDataJson: JsValue =
    Json.parse("""{
      "acknowledgementReference":"XN123434678569",
      "safeId":"243567679865",
      "emailConsent":false,
      "address":[{
        "name1":"a first name",
        "name2":"another name",
        "addressDetails":{
          "addressType":"Business",
          "addressLine1":"23 Springfield Drive",
          "addressLine2":"Springfield",
          "countryCode":"US"
        },
        "contactDetails":{
          "phoneNumber":"0121-345-234",
          "mobileNumber":"07124343234"
        }
      }],
      "utr":"123456789",
      "isNonUKClientRegisteredByAgent":true
      }
     """
    )

  val etmpCheckOrganisation: JsValue = Json.parse(
    """{
      |    "businessName": "ACME Limited",
      |    "businessType": "Corporate Body",
      |    "businessAddress": {
      |      "line_1": "1 Example Street",
      |      "line_2": "Example View",
      |      "line_3": "Example Town",
      |      "line_4": "Exampleshire",
      |      "postcode": "AA1 1AA",
      |      "country": "GB"
      |    },
      |    "sapNumber": "1234567890",
      |    "safeId": "XE0001234567890"
      |  }
      |""".stripMargin
  )

}
