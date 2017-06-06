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

package controllers

import config.FrontendAuthConnector
import connectors.BusinessCustomerFrontendConnector
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import org.joda.time.LocalDate
import services.RegisterUserService
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object AgentConfirmationController extends AgentConfirmationController {
  override val authConnector = FrontendAuthConnector
  val registerUserService: RegisterUserService = RegisterUserService
  override val businessCustomerFEConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
}

trait AgentConfirmationController extends FrontendController with Actions with RunMode {

  def registerUserService: RegisterUserService
  val businessCustomerFEConnector: BusinessCustomerFrontendConnector

  def view = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      businessCustomerFEConnector.getReviewDetails.map(reviewDetails =>
        Ok(views.html.agentConfirmation(reviewDetails.businessName, Dates.formatDate(LocalDate.now())))
      )

  }

  def continue = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      for {
        refreshResp <- registerUserService.refreshProfile
      } yield {
        refreshResp.status match {
          case OK | NO_CONTENT => Redirect(ExternalUrls.agentAtedSummaryPath)
          case _ => Redirect(ExternalUrls.logoutPath)
        }
      }
  }
}
