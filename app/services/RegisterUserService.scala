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
import models.{AtedSubscriptionAuthData, SubscribeSuccessResponse, Verifiers, _}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.OK
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.SessionUtils
import utils.BusinessTypeConstants.saBusinessTypes
import utils.GovernmentGatewayConstants._

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
                    ec: ExecutionContext): Future[SubscribeSuccessResponse] = {

    for {
      businessCustomerDetails <- registeredBusinessService.getBusinessCustomerDetails
      address <- dataCacheConnector.fetchCorrespondenceAddress
      contactDetails <- dataCacheConnector.fetchContactDetailsForSession
      contactDetailsEmail <- dataCacheConnector.fetchContactDetailsEmailForSession
      atedSubscription <- atedSubscriptionConnector.subscribeAted(
        prepareSubscriptionForAted(businessCustomerDetails, contactDetails, contactDetailsEmail, address, isNonUKClientRegisteredByAgent)
      )

    } yield {
      atedSubscription
    }
  }

  def enrolAted(registrationResponse: SubscribeSuccessResponse, isNonUKClientRegisteredByAgent: Boolean = false)
               (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier,
                request: Request[_], ec: ExecutionContext): Future[HttpResponse] = {
    if (isNonUKClientRegisteredByAgent) {
      val enrolResp = Json.toJson(EnrolResponse(serviceName = "ated", state = "NotEnroled", Nil))
      Future.successful(HttpResponse.apply(OK, enrolResp.toString))
    } else {
      for {
        authorised <- authConnector.authorise(AffinityGroup.Organisation, credentials and groupIdentifier)
        enrolSuccess <- authorised match {
          case Some(Credentials(ggCred, _)) ~ Some(groupId) =>
            if (registrationResponse.atedRefNumber.isEmpty) {
              throw new RuntimeException("[RegisterUserService][subscribeAted] ated reference number not returned from ETMP subscribe")
            } else {
              val grpId = appConfig.atedSubsUtils.validateGroupId(groupId)
              registeredBusinessService.getBusinessCustomerDetails flatMap { bcd =>
                val requestPayload = createEnrolmentRequest(
                  businessType = bcd.businessType,
                  gGCredId = ggCred,
                  utr = bcd.utr,
                  postcode = bcd.businessAddress.postcode,
                  safeId = bcd.safeId)
                taxEnrolmentsConnector.enrol(requestPayload, grpId, registrationResponse.atedRefNumber.get)
              }
            }
          case _ ~ None =>
            Future.failed(new RuntimeException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
          case _ =>
            Future.failed(new RuntimeException("Failed to enrol - authorisation exception"))
        }
      } yield {
        enrolSuccess
      }
    }
  }

  def getUtrType(businessType: String): String = {
    if (saBusinessTypes.contains(businessType)) {
      VerifierSaUtr
    } else {
      VerifierCtUtr
    }
  }

  def createEnrolmentRequest(businessType: String, gGCredId: String, utr: Option[String],
                             postcode: Option[String], safeId: String): RequestEMACPayload = {

    val utrType: String = getUtrType(businessType)

    RequestEMACPayload(
      userId = gGCredId,
      friendlyName = "ATED Enrolment",
      `type` = enrolmentType,
      verifiers = createEnrolmentVerifiers(utrType, utr, postcode))
  }

  def createEnrolmentVerifiers(utrType: String, utr: Option[String], postcode: Option[String]): Verifiers = {

    (utr, postcode) match {
      case (Some(uniqueTaxRef), Some(ukClientPostCode)) =>
        Verifiers(List(Verifier(VerifierPostalCode, ukClientPostCode), Verifier(utrType, uniqueTaxRef)))
      case (None, Some(nonUkClientPostCode)) =>
        Verifiers(List(
          Verifier(VerifierNonUKPostalCode, nonUkClientPostCode))
        ) //N.B. Non-UK Clients might use the property UK Postcode or their own Non-UK Postal Code
      case (Some(uniqueTaxRef), None) =>
        Verifiers(List(Verifier(utrType, uniqueTaxRef)))
      case (_, _) =>
        throw new RuntimeException(s"[RegisterUserService][createEnrolmentVerifiers] - postcode or utr must be supplied")
    }
  }

  private def prepareSubscriptionForAted(bcd: BusinessCustomerDetails, contactDetails: Option[ContactDetails],
                                         contactDetailsEmail: Option[ContactDetailsEmail],
                                         address: Option[Address], nonUKAgent: Boolean)
                                        (implicit auth: AtedSubscriptionAuthData, hc: HeaderCarrier): JsValue = {
    val contact = contactDetails.getOrElse(throw new RuntimeException("contact details not found"))
    val contactEmail = contactDetailsEmail.getOrElse(throw new RuntimeException("contact email not found"))
    val etmpAddress: EtmpAddressDetails = toEtmpAddress(address.getOrElse(throw new RuntimeException("address not found")))
    val etmpContactDetails: EtmpContactDetails = toEtmpContactDetails(contact, contactEmail)
    val correspondence: EtmpCorrespondence = EtmpCorrespondence(
      name1 = contact.firstName,
      name2 = contact.lastName,
      addressDetails = etmpAddress,
      contactDetails = etmpContactDetails)

    Json.toJson(AtedSubscriptionRequest(
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      safeId = bcd.safeId,
      emailConsent = contactEmail.emailConsent.getOrElse(false),
      address = List(correspondence),
      businessType = bcd.businessType,
      utr = bcd.utr,
      isNonUKClientRegisteredByAgent = nonUKAgent,
      knownFactPostcode = bcd.businessAddress.postcode)
    )
  }

  def toEtmpAddress(address: Address): EtmpAddressDetails = {
    EtmpAddressDetails(addressType = "Correspondence",
      addressLine1 = address.line_1,
      addressLine2 = address.line_2,
      addressLine3 = address.line_3,
      addressLine4 = address.line_4,
      postalCode = appConfig.atedSubsUtils.formatPostCode(address.postcode),
      countryCode = address.country)
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
