/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors._
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.OK
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.SessionUtils

import scala.concurrent.{ExecutionContext, Future}

class RegisterUserService @Inject()(appConfig: ApplicationConfig,
                                    atedSubscriptionConnector: AtedSubscriptionConnector,
                                    dataCacheConnector: AtedSubscriptionDataCacheConnector,
                                    registeredBusinessService: RegisteredBusinessService,
                                    taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                    val authConnector: DefaultAuthConnector
                                   ) extends AuthorisedFunctions {

  val enrolmentType = "principal"

  def subscribeAted(isNonUKClientRegisteredByAgent: Boolean = false)
                   (implicit user: AtedSubscriptionAuthData,
                    hc: HeaderCarrier, request: Request[_],
                    ec: ExecutionContext): Future[(SubscribeSuccessResponse, HttpResponse)] = {

    for {
      businessDetails <- registeredBusinessService.getBusinessCustomerDetails
      address <- dataCacheConnector.fetchCorrespondenceAddress
      contactDetails <- dataCacheConnector.fetchContactDetailsForSession
      contactDetailsEmail <- dataCacheConnector.fetchContactDetailsEmailForSession
      atedSubscriptionSuccess <- prepareSubscriptionForAted(contactDetails, contactDetailsEmail, address, businessDetails, isNonUKClientRegisteredByAgent)
      enrolResponse <- {
        if (isNonUKClientRegisteredByAgent) {
          val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))
          Future.successful(HttpResponse(OK, responseJson = Some(enrolResp)))
        } else {
          authConnector.authorise(AffinityGroup.Organisation, credentials and groupIdentifier) flatMap {
              case Credentials(ggCred, _) ~ Some(groupId) =>
                if (atedSubscriptionSuccess.atedRefNumber.isEmpty) {
                  throw new RuntimeException("[RegisterEmacUserService][createEMACEnrolRequest] ated reference number not returned from ETMP subscribe")
                } else {
                  val grpId = appConfig.atedSubsUtils.validateGroupId(groupId)
                  val requestPayload = createEMACEnrolRequest(businessDetails.businessType, ggCred,
                    businessDetails.utr, businessDetails.businessAddress.postcode,
                    businessDetails.safeId)
                  taxEnrolmentsConnector.enrol(requestPayload, grpId, atedSubscriptionSuccess.atedRefNumber.getOrElse(""))
                }
              case _ ~ None =>
                Future.failed(new RuntimeException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
          }
        }
      }
    } yield {
      (atedSubscriptionSuccess, enrolResponse)
    }
  }

  def createEMACEnrolRequest(businessType: Option[String], gGCredId: String, utr: Option[String],
                             postcode: Option[String], safeId : String): RequestEMACPayload = {
    def verifiers = (utr, postcode) match {
      case (Some(uniqueTaxRef), Some(ukClientPostCode)) =>
        List(
          Verifier("Postcode", ukClientPostCode),
          Verifier(verifierKeyForBusinessType(businessType), uniqueTaxRef)
        )
      //N.B. Non-UK Clients might use the property UK Postcode or their own Non-UK Postal Code
      case (None, Some(nonUkClientPostCode)) =>
        List(Verifier("NonUKPostalCode", nonUkClientPostCode))
      case (Some(uniqueTaxRef), None) =>
        Logger.info("[RegisterUserService][verifiers] Creating verifiers for user with no postcode")
        List(Verifier(verifierKeyForBusinessType(businessType), uniqueTaxRef))
      case (None, None) =>
        throw new RuntimeException(s"[RegisterUserService][subscribeAted][createEMACEnrolRequest] - postalCode or utr must be supplied")
    }

    RequestEMACPayload(userId = gGCredId,
      friendlyName = "ATED Enrolment",
      `type` = enrolmentType,
      verifiers = verifiers)
  }

  def verifierKeyForBusinessType(businessType: Option[String]): String = {
    businessType match {
      case Some("SOP") => "SAUTR"
      case _ => "CTUTR"
    }
  }

  private def prepareSubscriptionForAted(contactDetails: Option[ContactDetails], contactDetailsEmail: Option[ContactDetailsEmail],
                                         address: Option[Address], businessDetails: BusinessCustomerDetails,
                                         nonUKAgent: Boolean)
                                        (implicit auth: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[SubscribeSuccessResponse] = {
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
      isNonUKClientRegisteredByAgent = nonUKAgent,
      knownFactPostcode = businessDetails.businessAddress.postcode)
    val dataToSend: JsValue = Json.toJson(subscribeData)
    atedSubscriptionConnector.subscribeAted(data = dataToSend)
  }

  def toEtmpAddress(address: Address): EtmpAddressDetails = {
    val etmpAddress = EtmpAddressDetails(addressType = "Correspondence",
      addressLine1 = address.line_1,
      addressLine2 = address.line_2,
      addressLine3 = address.line_3,
      addressLine4 = address.line_4,
      postalCode = appConfig.atedSubsUtils.formatPostCode(address.postcode),
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