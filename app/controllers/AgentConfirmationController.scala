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

package controllers

import config.FrontendAuthConnector
import connectors.BusinessCustomerFrontendConnector
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import models.ReviewDetails
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.RegisterUserService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates

import scala.concurrent.Future

object AgentConfirmationController extends AgentConfirmationController {
  override val authConnector = FrontendAuthConnector
  val registerUserService: RegisterUserService = RegisterUserService
  override val businessCustomerFEConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
}

trait AgentConfirmationController extends FrontendController with Actions {

  def registerUserService: RegisterUserService
  val businessCustomerFEConnector: BusinessCustomerFrontendConnector

  def view = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      businessCustomerFEConnector.getReviewDetails.map(respone =>
        respone.status match {
          case OK =>
            val reviewDetails = respone.json.as[ReviewDetails]
            Ok(views.html.agentConfirmation(reviewDetails.businessName, Dates.formatDate(LocalDate.now())))
        }
      )
  }

  def continue = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      Future.successful(Redirect(ExternalUrls.agentAtedSummaryPath))
  }
}
