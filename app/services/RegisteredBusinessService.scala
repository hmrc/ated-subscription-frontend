/*
 * Copyright 2017 HM Revenue & Customs
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
import models.{SubscriptionData, _}
import play.api.mvc.Request
import play.mvc.Http.Status._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegisteredBusinessService {

  val businessCustomerFrontendConnector: BusinessCustomerFrontendConnector
  val atedConnector: AtedConnector
  val agentClientMandateFrontendConnector: AgentClientMandateFrontendConnector

  def getReviewBusinessDetails(implicit request: Request[_], user: AuthContext, hc: HeaderCarrier): Future[ReviewDetails] = {
    businessCustomerFrontendConnector.getReviewDetails flatMap  { response =>
      response.status match {
        case OK => Future.successful(response.json.as[ReviewDetails])
        case NOT_FOUND =>
          agentClientMandateFrontendConnector.getOldMandateDetails flatMap  { mandateRef =>
            val atedRefNumber = mandateRef.map(_.atedRefNumber).getOrElse(throw new RuntimeException("No Old Mandate Reference found for the client!"))
            atedConnector.retrieveSubscriptionData(atedRefNumber) flatMap { resp =>
              resp.status match {
                case OK =>
                  val subscriptionData = resp.json.as[SubscriptionData]
                  val addressData = subscriptionData.address.filter(_.addressDetails.addressType == "Default Place Of Business").head
                  val address = Address(line_1 = addressData.addressDetails.addressLine1,
                    line_2 = addressData.addressDetails.addressLine2,
                    country = addressData.addressDetails.countryCode)
                  Future.successful(ReviewDetails(businessName = subscriptionData.organisationName,
                    businessType = None, businessAddress = address, sapNumber = "", safeId = subscriptionData.safeId, agentReferenceNumber = user.principal.accounts.agent.flatMap(_.agentBusinessUtr.map(_.value))))
                case status => throw new RuntimeException(s"Error while retrieving subscription data for ated ref no: $atedRefNumber  status:: $status")
              }
            }
          }
        case _ => throw new RuntimeException(s"Error while retrieving review details from business-customer keystore")
      }
    }
  }

  def getDefaultCorrespondenceAddress(implicit request: Request[_], user: AuthContext, hc: HeaderCarrier): Future[Address] = {
    for {
      agentAddress <- getAgentCorrespondenceAddress
      correspondenceAddress <-
      agentAddress match {
        case Some(x) => Future.successful(x)
        case None => getBusinessAddress
      }
    } yield {
      correspondenceAddress
    }
  }

  def getBusinessAddress(implicit request: Request[_], user: AuthContext, hc: HeaderCarrier): Future[Address] = {
    getReviewBusinessDetails.map(_.businessAddress)
  }


  private def getAgentCorrespondenceAddress(implicit request: Request[_], user: AuthContext, hc: HeaderCarrier): Future[Option[Address]] = {

    def getDetails(identifier: String, identifierType: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[EtmpRegistrationDetails]] = {
      atedConnector.getDetails(identifier = identifier, identifierType = identifierType) map {
        response =>
          response.status match {
            case OK => response.json.asOpt[EtmpRegistrationDetails]
            case _ => None
          }
      }
    }

    val agentArn = user.principal.accounts.agent.flatMap(_.agentBusinessUtr.map(_.value))
    agentArn match {
      case Some(agentArnId) =>
        val IdentifierArn = "arn"
        for {
          foundDetails <- getDetails(agentArnId, IdentifierArn)
        } yield {
          foundDetails.map(details => Address(details.addressDetails.addressLine1,
            details.addressDetails.addressLine2,
            details.addressDetails.addressLine3,
            details.addressDetails.addressLine4,
            details.addressDetails.postalCode,
            details.addressDetails.countryCode))
        }
      case None => Future.successful(None)
    }
  }
}

object RegisteredBusinessService extends RegisteredBusinessService {
  val agentClientMandateFrontendConnector: AgentClientMandateFrontendConnector = AgentClientMandateFrontendConnector
  val businessCustomerFrontendConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
  val atedConnector: AtedConnector = AtedConnector
}
