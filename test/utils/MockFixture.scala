/*
 * Copyright 2025 HM Revenue & Customs
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

import models._
import org.scalatestplus.mockito.MockitoSugar
import services.{ContactDetailsService, CorrespondenceAddressService, MandateService, RegisteredBusinessService}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments, User}

trait MockFixture extends MockitoSugar {
  val mockRegisteredBusinessService: RegisteredBusinessService = mock[RegisteredBusinessService]
  val mockCorrespondenceAddressService: CorrespondenceAddressService = mock[CorrespondenceAddressService]
  val mockContactDetailsService: ContactDetailsService = mock[ContactDetailsService]
  val mockMandateService: MandateService = mock[MandateService]

  val testAddress: Address = Address("line_1", "line_2", None, None, None, "GB")
  val testAddress2: Address = Address("line_1", "line_2", Some("line_3"), Some("line_3"), Some("NE1 1AB"), "GB")
  val testIdentification: Identification = Identification(idNumber = "ID123", issuingInstitution = "InstTest", issuingCountryCode = "FR")
  val testReviewBusinessDetails: BusinessCustomerDetails = BusinessCustomerDetails(
    businessName = "test Name",
    businessType = "LLP",
    businessAddress = testAddress,
    sapNumber = "1234567890",
    safeId = "EX0012345678909",
    agentReferenceNumber = None,
    identification = Some(testIdentification)
  )

  implicit lazy val authContextAgent: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
    Some(User),
    Some(AffinityGroup.Agent),
    None,
    Some("credId"),
    Some("hashed"),
    Some("testGroupId-"),
    Enrolments(Set())
  )

  implicit lazy val authContextOrg: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
    Some(User),
    Some(AffinityGroup.Organisation),
    None,
    Some("credId"),
    Some("hashed"),
    Some("testGroupId-"),
    Enrolments(Set())
  )

  val mode: Option[String] = Some("skip")
  val testContact: ContactDetails = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail: ContactDetailsEmail = ContactDetailsEmail(Some(true), "abc@test.com")
  val testContactNoEmail: ContactDetailsEmail = ContactDetailsEmail(Some(false), "")
  val testContactLetter: ContactDetails = ContactDetails("ABC", "DEF", "1234567890")
  val emailAddress: AgentEmail = AgentEmail("test@mail.com")
  val clientDisplayName: ClientDisplayName = ClientDisplayName("client display name")

}
