/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.BusinessCustomerFrontendConnector
import controllers.auth.AuthFunctionality
import models.BusinessCustomerDetails
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Dates

import java.time.{ZoneId, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                            businessCustomerFEConnector: BusinessCustomerFrontendConnector,
                                            val authConnector: DefaultAuthConnector,
                                            template: views.html.agentConfirmation,
                                            templateError: views.html.global_error,
                                            implicit val appConfig: ApplicationConfig
                                           ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        businessCustomerFEConnector.getBusinessCustomerDetails.map(response =>
          response.status match {
            case OK =>
              val reviewDetails = response.json.as[BusinessCustomerDetails]
              Ok(template(reviewDetails.businessName, Dates.formatDate(ZonedDateTime.now(ZoneId.of("UTC")))))
            case status =>
              logger.error(s"[AgentConfirmationController][GetBusinessCustomerDetails] - $status - ${Option(response.body).getOrElse("No response body")}")
              InternalServerError(templateError(Messages("ated.business-registration.generic.error.title"),
                Messages("ated.business-registration.generic.error.header"), Messages("ated.business-registration.generic.error.message")))
          }
        )
      }
  }

  def continue: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(appConfig.agentAtedSummaryPath))
      }
  }
}
