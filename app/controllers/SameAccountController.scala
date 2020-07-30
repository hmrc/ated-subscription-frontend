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
import models.BusinessCustomerDetails
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates

import scala.concurrent.{ExecutionContext, Future}

class SameAccountController @Inject()(mcc: MessagesControllerComponents,
                                      businessCustomerFEConnector: BusinessCustomerFrontendConnector,
                                      val authConnector: DefaultAuthConnector,
                                      template: views.html.sameAccount,
                                      template2: views.html.inform,
                                      implicit val appConfig: ApplicationConfig
                                     ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def viewSameAccount: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(template(Some(controllers.routes.PreviousSubmittedController.view().url))))
      }
  }

  def viewInform: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(template2(Some(controllers.routes.SameAccountController.viewSameAccount().url))))
      }
  }

  def toNRLQuestionPage: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(appConfig.nrlPath))
      }
  }
}
