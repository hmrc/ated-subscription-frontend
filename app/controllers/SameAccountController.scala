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
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class SameAccountController @Inject()(mcc: MessagesControllerComponents,
                                      val authConnector: DefaultAuthConnector,
                                      templateSameAccount: views.html.sameAccount,
                                      templateInform: views.html.inform,
                                      implicit val appConfig: ApplicationConfig
                                     ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def viewSameAccount: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(templateSameAccount(Some(controllers.routes.PreviousSubmittedController.view().url))))
      }
  }

  def viewInform: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(templateInform(Some(controllers.routes.SameAccountController.viewSameAccount().url))))
      }
  }

  def toNRLQuestionPage: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(appConfig.nrlPath))
      }
  }
}
