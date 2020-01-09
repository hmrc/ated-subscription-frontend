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

package controllers

import config.ApplicationConfig
import connectors.BusinessCustomerFrontendConnector
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import models.ReviewDetails
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates

import scala.concurrent.{ExecutionContext, Future}

class AgentConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                            businessCustomerFEConnector: BusinessCustomerFrontendConnector,
                                            val authConnector: DefaultAuthConnector,
                                            implicit val appConfig: ApplicationConfig
                                           ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        businessCustomerFEConnector.getReviewDetails.map(response =>
          response.status match {
            case OK =>
              val reviewDetails = response.json.as[ReviewDetails]
              Ok(views.html.agentConfirmation(reviewDetails.businessName, Dates.formatDate(LocalDate.now())))
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
