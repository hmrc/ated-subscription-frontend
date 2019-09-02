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

package controllers.nonUKReg

import config.AuthClientConnector
import controllers.auth.{AuthFunctionality, ExternalUrls}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.RegisteredBusinessService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates

import scala.concurrent.Future

trait ConfirmationController extends FrontendController with AuthFunctionality {

  def registeredBusinessService: RegisteredBusinessService

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        registeredBusinessService.getReviewBusinessDetails.map(_.businessName) map { name =>
          Ok(views.html.nonUKReg.confirmation(name, Dates.formatDate(LocalDate.now())))
        }
      }
  }

  def continue: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(ExternalUrls.agentAtedSummaryPath))
      }
  }

}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  val authConnector = AuthClientConnector
  val registeredBusinessService: RegisteredBusinessService = RegisteredBusinessService
  // $COVERAGE-ON$
}
