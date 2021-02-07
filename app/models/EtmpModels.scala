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

import play.api.libs.json._

case class SubscribeSuccessResponse(processingDate: Option[String], atedRefNumber: Option[String], formBundleNumber: Option[String])

object SubscribeSuccessResponse {
  implicit val formats: OFormat[SubscribeSuccessResponse] = Json.format[SubscribeSuccessResponse]
}

case class SelfHealSubscriptionResponse(regimeRefNumber: String)

object SelfHealSubscriptionResponse {
  implicit val formats: OFormat[SelfHealSubscriptionResponse] = Json.format[SelfHealSubscriptionResponse]
}

case class EtmpAddressDetails(addressType: String,
                       addressLine1: String,
                       addressLine2: String,
                       addressLine3: Option[String],
                       addressLine4: Option[String],
                       postalCode: Option[String],
                       countryCode: String)


object EtmpAddressDetails {

  implicit class FormatOps[T] (f: OFormat[T]) {

    def strictNull[V:Format](key: String, get: T => Option[V]): OFormat[T] = new OFormat[T] {
      def reads(j: JsValue): JsResult[T] = f.reads(j)

      def writes(u: T): JsObject =
        (f.writes(u) - key) ++ Json.obj(
          key -> (get(u).map(implicitly[Format[V]].writes _).getOrElse(JsNull): JsValue)
        )
    }
  }

  implicit val formats: Format[EtmpAddressDetails] = Json.format[EtmpAddressDetails]
    .strictNull("postalCode", _.postalCode)
}

case class EtmpContactDetails(phoneNumber: Option[String],
                              mobileNumber: Option[String],
                              faxNumber: Option[String],
                              emailAddress: Option[String])

object EtmpContactDetails {
  implicit val formats: OFormat[EtmpContactDetails] = Json.format[EtmpContactDetails]
}

case class EtmpCorrespondence(name1: String,
                              name2: String,
                              addressDetails: EtmpAddressDetails,
                              contactDetails: EtmpContactDetails)

object EtmpCorrespondence {
  implicit val formats: OFormat[EtmpCorrespondence] = Json.format[EtmpCorrespondence]
}

case class AtedSubscriptionRequest(acknowledgementReference: String,
                                   safeId: String,
                                   emailConsent: Boolean,
                                   address: List[EtmpCorrespondence],
                                   businessType: String,
                                   utr: Option[String],
                                   isNonUKClientRegisteredByAgent : Boolean,
                                   knownFactPostcode: Option[String])

object AtedSubscriptionRequest {
  implicit val formats: OFormat[AtedSubscriptionRequest] = Json.format[AtedSubscriptionRequest]
}
