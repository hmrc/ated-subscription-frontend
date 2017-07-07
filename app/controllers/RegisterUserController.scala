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
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import org.joda.time.LocalDate
import play.api.Logger
import services.RegisterUserService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates
import utils.AuthUtils._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import utils.ErrorMessageUtils._

import scala.concurrent.Future

trait RegisterUserController extends FrontendController with Actions {

  val registerUserService: RegisterUserService

  def registerUser = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      if (isAgent) Future.successful(Redirect(controllers.nonUKReg.routes.DeclarationController.view()))
      else registerUserService.subscribeAted() flatMap { registerResponse =>
        (registerResponse._1, registerResponse._2.status) match {
          case (_, BAD_GATEWAY) =>
            val errorMessage = formatErrorMessage(parseErrorResp(registerResponse._2)).getOrElse {
              Logger.warn(s"[RegisterUserController][registerUser] - Exception - No matching GG ErrorNumbers")
              throw new RuntimeException(Messages("ated.business-registration.error.bad.gateway.other"))
            }
            Logger.warn(s"[RegisterUserController][registerUser] - Exception - While GG enrolment")
            Future.successful(Ok(views.html.global_error(errorMessage._1, errorMessage._2, errorMessage._3)))
          case (_, _) => Future.successful((Redirect(controllers.routes.RegisterUserController.confirmation())))
        }
      }
  }

  def confirmation = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      for {
        refreshResp <- registerUserService.refreshProfile
      } yield {
        Ok(views.html.registerUserConfirmation(Dates.formatDate(LocalDate.now())))
      }
  }

  def redirectToAted = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      Redirect(ExternalUrls.clientSummaryPath)
  }

  private def formatErrorMessage(errorNum: String): Option[(String, String, String)] = {
    errorNum match {
      case "9001" | "11006" | "10004" => Some(Messages("ated.business-registration-error.duplicate.identifier.header"),
    Messages("ated.business-registration-error.duplicate.identifier.title"),
    Messages("ated.business-registration-error.duplicate.identifier.message"))
      case "8026" => Some(Messages("ated.business-registration-error.wrong.role.header"),
    Messages("ated.business-registration-error.wrong.role.title"),
    Messages("ated.business-registration-error.wrong.role.message"))
      case _ => None
    }
  }

}


object RegisterUserController extends RegisterUserController {
  val authConnector = FrontendAuthConnector
  val registerUserService = RegisterUserService
}