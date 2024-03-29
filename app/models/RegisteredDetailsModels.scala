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

case class RegisteredAddressDetails(addressLine1: String,
                                    addressLine2: String,
                                    addressLine3: Option[String] = None,
                                    addressLine4: Option[String] = None,
                                    postalCode: Option[String] = None,
                                    countryCode: String)

object RegisteredAddressDetails {
  implicit val formats: OFormat[RegisteredAddressDetails] = Json.format[RegisteredAddressDetails]
}

case class EtmpRegistrationDetails(sapNumber: String,
                                   safeId: String,
                                   agentReferenceNumber: Option[String],
                                   addressDetails: RegisteredAddressDetails) {
}

object EtmpRegistrationDetails {
  implicit val formats: OFormat[EtmpRegistrationDetails] = Json.format[EtmpRegistrationDetails]
}
