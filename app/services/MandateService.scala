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

import connectors._
import javax.inject.Inject
import models._
import play.api.http.Status._
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class MandateService @Inject()(dataCacheConnector: AtedSubscriptionDataCacheConnector,
                               mandateConnector: AgentClientMandateConnector,
                               mandateFrontendConnector: AgentClientMandateFrontendConnector,
                               registeredBusinessService: RegisteredBusinessService) {


  def createMandateForNonUK(atedRefNum: String)
                           (implicit hc: HeaderCarrier, user: AtedSubscriptionAuthData, request: Request[_], ec: ExecutionContext): Future[HttpResponse] = {
    val contactDetailsFuture = dataCacheConnector.fetchContactDetailsForSession
    val contactDetailsEmailFuture = dataCacheConnector.fetchContactDetailsEmailForSession
    val mandateDataFuture = fetchEmailAddress
    val clientDisplayNameFuture = fetchClientDisplayName
    val reviewDetailsFuture = registeredBusinessService.getBusinessCustomerDetails
    for {
      _ <- contactDetailsFuture
      contactDetailsEmail <- contactDetailsEmailFuture
      mandateData <- mandateDataFuture
      reviewDetail <- reviewDetailsFuture
      clientDisplayNameData <- clientDisplayNameFuture
      mandateResponse <- {
        val agentEmail = mandateData.fold("") {_.email}
        val safeId = reviewDetail.safeId
        val clientEmail = contactDetailsEmail.map(_.email).getOrElse("")
        val clientDisplayName = clientDisplayNameData.fold("") {_.name}
        val dto = NonUKClientDto(
          safeId = safeId,
          subscriptionReference = atedRefNum,
          service = "ated",
          clientEmail = clientEmail,
          arn = AuthUtils.getArn,
          agentEmail = agentEmail,
          clientDisplayName
        )
        mandateConnector.createMandateForNonUK(dto)
      }
    } yield {
      mandateResponse.status match {
        case CREATED => mandateResponse
        case status => throw new RuntimeException("Mandate creation failed.")
      }
    }
  }

  def updateMandateForNonUK(atedRefNum: String, mandateId: String)
                           (implicit hc: HeaderCarrier, user: AtedSubscriptionAuthData, request: Request[_], ec: ExecutionContext): Future[HttpResponse] = {
    val contactDetailsFuture = dataCacheConnector.fetchContactDetailsForSession
    val contactDetailsEmailFuture = dataCacheConnector.fetchContactDetailsEmailForSession
    val mandateDataFuture = fetchEmailAddress
    val clientDisplayNameFuture = fetchClientDisplayName
    val reviewDetailsFuture = registeredBusinessService.getBusinessCustomerDetails
    for {
      _ <- contactDetailsFuture
      contactDetailsEmail <- contactDetailsEmailFuture
      mandateData <- mandateDataFuture
      reviewDetail <- reviewDetailsFuture
      clientDisplayNameData <- clientDisplayNameFuture
      mandateResponse <- {
        val agentEmail = mandateData.fold("") {_.email}
        val safeId = reviewDetail.safeId
        val clientEmail = contactDetailsEmail.map(_.email).getOrElse("")
        val clientDisplayName = clientDisplayNameData.fold("") {_.name}
        val dto = NonUKClientDto(
          safeId = safeId,
          subscriptionReference = atedRefNum,
          service = "ated",
          clientEmail = clientEmail,
          arn = AuthUtils.getArn,
          agentEmail = agentEmail,
          clientDisplayName,
          mandateRef = Some(mandateId)
        )
        mandateConnector.updateMandateForNonUK(dto)
      }
    } yield {
      mandateResponse.status match {
        case CREATED => mandateResponse
        case _ => throw new RuntimeException("Non-UK Mandate update failed.")
      }
    }
  }

  def fetchEmailAddress(implicit request: Request[_], user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentEmail]] = {
    if (AuthUtils.isAgent) mandateFrontendConnector.getAgentEmail else Future.successful(None)
  }

  def fetchClientDisplayName(implicit request: Request[_], user: AtedSubscriptionAuthData,
                             hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ClientDisplayName]] = {
    if (AuthUtils.isAgent) mandateFrontendConnector.getClientDisplayName else Future.successful(None)
  }

}
