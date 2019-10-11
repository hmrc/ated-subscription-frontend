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

import play.api.libs.json.{Json, OFormat}

case class AreYouAnAgent(isAgent: Option[Boolean] = None)

object AreYouAnAgent {
  implicit val formats: OFormat[AreYouAnAgent] = Json.format[AreYouAnAgent]
}

case class AppointAgentForm(isAgent: Option[Boolean] = None)

object AppointAgentForm {
  implicit val formats: OFormat[AppointAgentForm] = Json.format[AppointAgentForm]
}

case class Address(
                    line_1: String,
                    line_2: String,
                    line_3: Option[String] = None,
                    line_4: Option[String] = None,
                    postcode: Option[String] = None,
                    country: String) {

  override def toString: String = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}


object Address {
  implicit val formats: OFormat[Address] = Json.format[Address]
}

case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object Identification {
  implicit val formats: OFormat[Identification] = Json.format[Identification]
}

case class ReviewDetails(businessName: String,
                         businessType: Option[String],
                         businessAddress: Address,
                         sapNumber: String,
                         safeId: String,
                         isAGroup: Boolean = false,
                         directMatch: Boolean = false,
                         agentReferenceNumber: Option[String],
                         firstName: Option[String] = None,
                         lastName: Option[String] = None,
                         utr: Option[String] = None,
                         identification: Option[Identification] = None,
                         isBusinessDetailsEditable: Boolean = false)

object ReviewDetails {
  implicit val formats: OFormat[ReviewDetails] = Json.format[ReviewDetails]
}

case class BusinessAddress(isCorrespondenceAddress: Option[Boolean] = None)

object BusinessAddress {
  implicit val formats: OFormat[BusinessAddress] = Json.format[BusinessAddress]
}

case class ContactDetails(firstName: String,
                          lastName: String,
                          telephone: String)

object ContactDetails {
  implicit val formats: OFormat[ContactDetails] = Json.format[ContactDetails]
}

case class ContactDetailsEmail(emailConsent: Option[Boolean] = None,
                          email: String
                          )

object ContactDetailsEmail {
  implicit val formats: OFormat[ContactDetailsEmail] = Json.format[ContactDetailsEmail]
}

case class NonUKClientDto(
                           safeId: String,
                           subscriptionReference: String,
                           service: String,
                           clientEmail: String,
                           arn: String,
                           agentEmail: String,
                           clientDisplayName: String,
                           mandateRef: Option[String] = None
                         )

object NonUKClientDto {
  implicit val formats: OFormat[NonUKClientDto] = Json.format[NonUKClientDto]
}

case class AgentEmail(email: String)

object AgentEmail {
  implicit val formats: OFormat[AgentEmail] = Json.format[AgentEmail]
}

case class ClientDisplayName(name: String)

object ClientDisplayName {
  implicit val formats: OFormat[ClientDisplayName] = Json.format[ClientDisplayName]
}
