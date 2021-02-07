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

package services

import connectors.{AgentClientMandateFrontendConnector, AtedConnector, BusinessCustomerFrontendConnector}
import javax.inject.Inject
import models.{Address, AtedSubscriptionAuthData, BusinessCustomerDetails, EtmpRegistrationDetails, SubscriptionData}
import play.api.mvc.Request
import play.mvc.Http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessService @Inject()(businessCustomerFrontendConnector: BusinessCustomerFrontendConnector,
                                          atedConnector: AtedConnector,
                                          agentClientMandateFrontendConnector: AgentClientMandateFrontendConnector)  {

  def getBusinessCustomerDetails(implicit request: Request[_], user: AtedSubscriptionAuthData,
                                 hc: HeaderCarrier, ec: ExecutionContext): Future[BusinessCustomerDetails] = {
    businessCustomerFrontendConnector.getBusinessCustomerDetails flatMap { response =>
      response.status match {
        case OK => {
          Future.successful(response.json.as[BusinessCustomerDetails])
        }
        case NOT_FOUND =>
          agentClientMandateFrontendConnector.getOldMandateDetails flatMap  { mandateRef =>
            val atedRefNumber = mandateRef.map(_.atedRefNumber).getOrElse(throw new RuntimeException("No Old Mandate Reference found for the client!"))
            atedConnector.retrieveSubscriptionData(atedRefNumber) flatMap { resp =>
              resp.status match {
                case OK =>
                  val subscriptionData = resp.json.as[SubscriptionData]
                  val addressData = subscriptionData.address.filter(_.addressDetails.addressType == "Permanent Place Of Business").head
                  val address = Address(line_1 = addressData.addressDetails.addressLine1,
                    line_2 = addressData.addressDetails.addressLine2,
                    country = addressData.addressDetails.countryCode)
                  val agentRefNo = user.enrolments.getEnrolment("HMRC-AGENT-AGENT").flatMap(_.getIdentifier("AgentRefNumber").map(_.value))
                  Future.successful(BusinessCustomerDetails(businessName = subscriptionData.organisationName, businessType = "Partnership",
                    businessAddress = address, sapNumber = "", safeId = subscriptionData.safeId, agentReferenceNumber = agentRefNo))
                case status => throw new RuntimeException(s"Error while retrieving subscription data for ated ref no: $atedRefNumber  status:: $status")
              }
            }
          }
        case status => throw new RuntimeException(s"Error while retrieving review details from business-customer keystore with status:$status")
      }
    }
  }

  def getDefaultCorrespondenceAddress(businessAddress: Option[Address] = None)(implicit request: Request[_],
                                      user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[Address] = {
    getAgentCorrespondenceAddress flatMap {
      case Some(address) => Future.successful(address)
      case _             => businessAddress.fold(getBusinessAddress)(Future.successful)
    }
  }

  def getBusinessAddress(implicit request: Request[_], user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[Address] = {
    getBusinessCustomerDetails.map(_.businessAddress)
  }

  private def getAgentCorrespondenceAddress(implicit request: Request[_],
                                            user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Address]] = {

    def getDetails(identifier: String, identifierType: String)
                  (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier): Future[Option[EtmpRegistrationDetails]] = {
      atedConnector.getDetails(identifier = identifier, identifierType = identifierType) map { response =>
          response.status match {
            case OK => response.json.asOpt[EtmpRegistrationDetails]
            case _ => None
          }
      }
    }

    val agentArn = user.enrolments.getEnrolment("HMRC-AGENT-AGENT").flatMap(_.getIdentifier("AgentRefNumber").map(_.value))
    agentArn match {
      case Some(agentArnId) =>
        val IdentifierArn = "arn"
        for {
          foundDetails <- getDetails(agentArnId, IdentifierArn)
        } yield {
          foundDetails.map { details =>
            Address(
              details.addressDetails.addressLine1,
              details.addressDetails.addressLine2,
              details.addressDetails.addressLine3,
              details.addressDetails.addressLine4,
              details.addressDetails.postalCode,
              details.addressDetails.countryCode
            )
          }
        }
      case None => Future.successful(None)
    }
  }
}
