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

import connectors._
import models.{AgentEmail, ClientDisplayName, ContactDetails, NonUKClientDto}
import play.api.http.Status._
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MandateService extends MandateService {
  // $COVERAGE-OFF$
  val dataCacheConnector = AtedSubscriptionDataCacheConnector
  val mandateConnector: AgentClientMandateConnector = AgentClientMandateConnector
  val mandateFrontendConnector = AgentClientMandateFrontendConnector
  val registeredBusinessService = RegisteredBusinessService
  // $COVERAGE-ON$
}

trait MandateService {

  val dataCacheConnector: DataCacheConnector

  def mandateConnector: AgentClientMandateConnector

  def mandateFrontendConnector: AgentClientMandateFrontendConnector

  def registeredBusinessService: RegisteredBusinessService

  def createMandateForNonUK(atedRefNum: String)(implicit hc: HeaderCarrier, user: AuthContext, request: Request[_]): Future[HttpResponse] = {
    val contactDetailsFuture = dataCacheConnector.fetchContactDetailsForSession
    val contactDetailsEmailFuture = dataCacheConnector.fetchContactDetailsEmailForSession
    val mandateDataFuture = fetchEmailAddress
    val clientDisplayNameFuture = fetchClientDisplayName
    val reviewDetailsFuture = registeredBusinessService.getReviewBusinessDetails
    for {
      contactDetails <- contactDetailsFuture
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

  def fetchEmailAddress(implicit request: Request[_], user: AuthContext): Future[Option[AgentEmail]] = {
    if (AuthUtils.isAgent) mandateFrontendConnector.getAgentEmail else Future.successful(None)
  }

  def fetchClientDisplayName(implicit request: Request[_], user: AuthContext): Future[Option[ClientDisplayName]] = {
    if (AuthUtils.isAgent) mandateFrontendConnector.getClientDisplayName else Future.successful(None)
  }

}