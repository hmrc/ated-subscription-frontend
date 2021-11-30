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

package controllers

import config.ApplicationConfig
import connectors.BusinessCustomerFrontendConnector
import controllers.auth.AuthFunctionality
import forms.AtedForms
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.OverseasCompanyService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class PreviousSubmittedController @Inject()(mcc: MessagesControllerComponents,
                                            businessCustomerFEConnector: BusinessCustomerFrontendConnector,
                                            overseasCompanyService: OverseasCompanyService,
                                            val authConnector: DefaultAuthConnector,
                                            template: views.html.previous_submitted,
                                            implicit val appConfig: ApplicationConfig
                                           ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        overseasCompanyService.fetchPreviouslySubmitted.map { optPreviouslySubmitted =>
          val form = optPreviouslySubmitted match {
            case Some(answer) => AtedForms.previousSubmittedForm.fill(answer)
            case _ => AtedForms.previousSubmittedForm
          }

          Ok(template(form, Some(appConfig.backToBusinessCustomerUrl)))
        }
      }
  }

  def continue: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        AtedForms.previousSubmittedForm.bindFromRequest().fold(
          hasErrors =>
            Future.successful(BadRequest(template(hasErrors, Some(appConfig.backToBusinessCustomerUrl)))),
          success => {
            val answer = success.isPreviousSubmitted
            answer match {
              case Some(prevSubmitted) =>
                overseasCompanyService.savePreviouslySubmitted(success) map { _ =>
                  if (prevSubmitted) {
                    Redirect(routes.SameAccountController.viewSameAccount)
                  } else {
                    Redirect(appConfig.nrlPath)
                  }
                }
              case _ => Future.successful(Redirect(routes.PreviousSubmittedController.view))
            }
          }
        )
      }
  }
}
