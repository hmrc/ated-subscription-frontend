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

import play.api.libs.json.{Json, OFormat}

case class SubscriptionContactDetails(phoneNumber: Option[String] = None,
                          mobileNumber: Option[String] = None,
                          faxNumber: Option[String] = None,
                          emailAddress: Option[String] = None)

object SubscriptionContactDetails {
  implicit val formats: OFormat[SubscriptionContactDetails] = Json.format[SubscriptionContactDetails]
}

case class AddressDetails(addressType: String,
                          addressLine1: String,
                          addressLine2: String,
                          addressLine3: Option[String] = None,
                          addressLine4: Option[String] = None,
                          postalCode: Option[String] = None,
                          countryCode: String)

object AddressDetails {
  implicit val formats: OFormat[AddressDetails] = Json.format[AddressDetails]
}

case class SubscriptionAddress(name1: Option[String] = None,
                   name2: Option[String] = None,
                   addressDetails: AddressDetails,
                   contactDetails: Option[SubscriptionContactDetails] = None)

object SubscriptionAddress {
  implicit val formats: OFormat[SubscriptionAddress] = Json.format[SubscriptionAddress]
}

case class SubscriptionData(safeId: String, organisationName: String, emailConsent: Option[Boolean], address : Seq[SubscriptionAddress])

object SubscriptionData {
  implicit val formats: OFormat[SubscriptionData] = Json.format[SubscriptionData]
}
