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

package services

import config.AuthClientConnector
import connectors._
import models._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.OK
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AtedSubscriptionUtils.validateGroupId
import utils.{AtedSubscriptionUtils, GovernmentGatewayConstants, SessionUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait RegisterUserService extends AuthorisedFunctions {

  val atedSubscriptionConnector: AtedSubscriptionConnector
  val dataCacheConnector: DataCacheConnector
  val registeredBusinessService: RegisteredBusinessService
  val taxEnrolmentsConnector : TaxEnrolmentsConnector
  val enrolmentType = "principal"

  def subscribeAted(isNonUKClientRegisteredByAgent: Boolean = false)(implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, request: Request[_]): Future[(SubscribeSuccessResponse, HttpResponse)] = {
    for {
      businessDetails <- registeredBusinessService.getReviewBusinessDetails
      address <- dataCacheConnector.fetchCorrespondenceAddress
      contactDetails <- dataCacheConnector.fetchContactDetailsForSession
      contactDetailsEmail <- dataCacheConnector.fetchContactDetailsEmailForSession
      atedSubscriptionSuccess <- {
        val contact = contactDetails.getOrElse(throw new RuntimeException("data not found"))
        val contactEmail = contactDetailsEmail.getOrElse(throw new RuntimeException("data not found"))
        val etmpAddress: EtmpAddressDetails = toEtmpAddress(address.getOrElse(throw new RuntimeException("data not found")))
        val etmpContactDetails: EtmpContactDetails = toEtmpContactDetails(contact, contactEmail)
        val correspondence: EtmpCorrespondence = EtmpCorrespondence(name1 = contact.firstName,
          name2 = contact.lastName,
          addressDetails = etmpAddress,
          contactDetails = etmpContactDetails)

        val subscribeData: SubscribeData = SubscribeData(acknowledgementReference = SessionUtils.getUniqueAckNo,
          safeId = businessDetails.safeId,
          address = List(correspondence),
          emailConsent = contactEmail.emailConsent.getOrElse(false),
          utr = businessDetails.utr.getOrElse(""),
          isNonUKClientRegisteredByAgent = isNonUKClientRegisteredByAgent,
          knownFactPostcode = businessDetails.businessAddress.postcode)
        val dataToSend: JsValue = Json.toJson(subscribeData)
        atedSubscriptionConnector.subscribeAted(data = dataToSend)
      }
      enrolResponse <- {
        if (isNonUKClientRegisteredByAgent) {
          val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))
          Future.successful(HttpResponse(OK, responseJson = Some(enrolResp)))
        } else {
              authConnector.authorise(AffinityGroup.Organisation, credentials and groupIdentifier) flatMap {
              case Credentials(ggCred, _) ~ Some(groupId) =>
                val grpId = validateGroupId(groupId)
                val requestPayload = createEMACEnrolRequest(atedSubscriptionSuccess,ggCred,
                  businessDetails.utr, businessDetails.businessAddress.postcode,
                  businessDetails.safeId)
                  taxEnrolmentsConnector.enrol(requestPayload, grpId, atedSubscriptionSuccess.atedRefNumber.getOrElse(""))
              case _ ~ None =>
                Future.failed(new RuntimeException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
          }
        }
      }
    } yield {
      (atedSubscriptionSuccess, enrolResponse)
    }
  }

  private def createEMACEnrolRequest(atedSubscriptionSuccess: SubscribeSuccessResponse,
                                     gGCredId: String, utr: Option[String], postcode: Option[String],
                                     safeId : String): RequestEMACPayload = {
    val atedRef = atedSubscriptionSuccess.atedRefNumber
      .getOrElse(throw new RuntimeException("[RegisterEmacUserService][createEMACEnrolRequest] ated reference number not returned from ETMP subscribe"))

    def verifiers = (utr, postcode) match {
      case (Some(uniqueTaxRef), Some(ukClientPostCode)) =>
        List(Verifier(GovernmentGatewayConstants.VerifierPostalCode, ukClientPostCode), Verifier(GovernmentGatewayConstants.VerifierCtUtr, uniqueTaxRef))
      case (None, Some(nonUkClientPostCode)) =>
        List(Verifier(GovernmentGatewayConstants.VerifierNonUKPostalCode, nonUkClientPostCode)) //N.B. Non-UK Clients might use the property UK Postcode or their own Non-UK Postal Code
      case (Some(uniqueTaxRef), None) =>
        throw new RuntimeException(s"[RegisterUserService][subscribeAted][createEMACEnrolRequest] - postalCode must be supplied")
      case (None, None) =>
        throw new RuntimeException(s"[RegisterUserService][subscribeAted][createEMACEnrolRequest] - postalCode or utr must be supplied")
    }

    RequestEMACPayload(userId = gGCredId,
      friendlyName = GovernmentGatewayConstants.FRIENDLY_NAME,
      `type` = enrolmentType,
      verifiers = verifiers)
  }

  def toEtmpAddress(address: Address): EtmpAddressDetails = {
    val etmpAddress = EtmpAddressDetails(addressType = "Correspondence",
      addressLine1 = address.line_1,
      addressLine2 = address.line_2,
      addressLine3 = address.line_3,
      addressLine4 = address.line_4,
      postalCode = AtedSubscriptionUtils.formatPostCode(address.postcode),
      countryCode = address.country)
    etmpAddress
  }

  def toEtmpContactDetails(contact: ContactDetails, contactEmail: ContactDetailsEmail): EtmpContactDetails = {
    val email = if (contactEmail.email.trim.isEmpty) None else Some(contactEmail.email.trim)
    val telephone = if (contact.telephone.trim.isEmpty) None else Some(contact.telephone.trim)

    EtmpContactDetails(
      phoneNumber = telephone,
      mobileNumber = None,
      faxNumber = None,
      emailAddress = email)
  }

}

object RegisterUserService extends RegisterUserService {
  // $COVERAGE-OFF$
  val registeredBusinessService = RegisteredBusinessService
  val atedSubscriptionConnector = AtedSubscriptionConnector
  val dataCacheConnector = AtedSubscriptionDataCacheConnector
  override val authConnector = AuthClientConnector
  val taxEnrolmentsConnector: TaxEnrolmentsConnector = TaxEnrolmentsConnector
  // $COVERAGE-ON$
}
