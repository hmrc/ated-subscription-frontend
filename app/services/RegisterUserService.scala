/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.OK
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AtedSubscriptionUtils, GovernmentGatewayConstants, SessionUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.play.config.RunMode


trait RegisterUserService extends RunMode {

  val atedSubscriptionConnector: AtedSubscriptionConnector
  val dataCacheConnector: DataCacheConnector
  val governmentGatewayConnector: GovernmentGatewayConnector
  val authenticatorConnector: AuthenticatorConnector
  val registeredBusinessService: RegisteredBusinessService


  def subscribeAted(isNonUKClientRegisteredByAgent: Boolean = false)(implicit user: AuthContext, hc: HeaderCarrier, request: Request[_]): Future[(SubscribeSuccessResponse, HttpResponse)] = {
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
            val enrolmentReq = createEnrolRequest(atedSubscriptionSuccess,
              businessDetails.businessAddress.postcode.getOrElse(""), businessDetails.utr.getOrElse(""))
            governmentGatewayConnector.enrol(enrolmentReq)

        }
      }
    } yield {
      (atedSubscriptionSuccess, enrolResponse)
    }
  }

  def refreshProfile(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    authenticatorConnector.refreshProfile()
  }

  private def createEnrolRequest(atedSubscriptionSuccess: SubscribeSuccessResponse, postcode: String, utr: String): EnrolRequest = {
    val atedRef = atedSubscriptionSuccess.atedRefNumber
      .getOrElse(throw new RuntimeException("[RegisterUserService][createEnrolRequest] ated reference number not returned from ETMP subscribe"))

    EnrolRequest(portalId = GovernmentGatewayConstants.ATED_PORTAL_IDENTIFIER,
      serviceName = GovernmentGatewayConstants.ATED_SERVICE_NAME,
      friendlyName = GovernmentGatewayConstants.FRIENDLY_NAME,
      knownFacts = Seq(atedRef, utr, "", postcode))
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
  private def createVerifiers(safeId: String, utr: Option[String], businessType: String, postcode: String) = {
    val utrVerifier = businessType match {
      case "SOP" => Verifier("SAUTR", utr.getOrElse(""))
      case _ => Verifier("CTUTR", utr.getOrElse(""))
    }
    List(
      Verifier("Postcode", postcode),
      Verifier("SAFEID", safeId)
    ) :+ utrVerifier
  }

}

object RegisterUserService extends RegisterUserService {
  val registeredBusinessService = RegisteredBusinessService
  val atedSubscriptionConnector = AtedSubscriptionConnector
  val dataCacheConnector = AtedSubscriptionDataCacheConnector
  val governmentGatewayConnector = GovernmentGatewayConnector
  val authenticatorConnector = AuthenticatorConnector
}
