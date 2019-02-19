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

package models

import play.api.libs.json.Json

case class SubscribeSuccessResponse(processingDate: Option[String], atedRefNumber: Option[String], formBundleNumber: Option[String])

object SubscribeSuccessResponse {
  implicit val formats = Json.format[SubscribeSuccessResponse]
}

case class EtmpAddressDetails(addressType: String,
                       addressLine1: String,
                       addressLine2: String,
                       addressLine3: Option[String],
                       addressLine4: Option[String],
                       postalCode: Option[String],
                       countryCode: String)

object EtmpAddressDetails {
  implicit val formats = Json.format[EtmpAddressDetails]
}

case class EtmpContactDetails(phoneNumber: Option[String],
                              mobileNumber: Option[String],
                              faxNumber: Option[String],
                              emailAddress: Option[String])

object EtmpContactDetails {
  implicit val formats = Json.format[EtmpContactDetails]
}

case class EtmpCorrespondence(name1: String,
                              name2: String,
                              addressDetails: EtmpAddressDetails,
                              contactDetails: EtmpContactDetails)

object EtmpCorrespondence {
  implicit val formats = Json.format[EtmpCorrespondence]
}

case class SubscribeData(acknowledgementReference: String,
                         safeId: String,
                         emailConsent: Boolean,
                         address: List[EtmpCorrespondence],
                         utr: String,
                         isNonUKClientRegisteredByAgent : Boolean,
                         knownFactPostcode: Option[String])

object SubscribeData {
  implicit val formats = Json.format[SubscribeData]
}
