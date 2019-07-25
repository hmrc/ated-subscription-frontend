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
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.{Logger, Play}
import services.{RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates
import utils.AuthUtils._
import utils.ErrorMessageUtils._

import scala.concurrent.Future

trait RegisterUserController extends FrontendController with Actions {


  val registerUserService: RegisterUserService

  private val DuplicateUserError = "duplicate user error"
  private val WrongRoleUserError = "wrong role user error"

  def registerUser = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        if (isAgent) Future.successful(Redirect(controllers.nonUKReg.routes.DeclarationController.view()))
        else {
            registerUserService.subscribeAted() map { registerResponse =>
              val (etmpSuccesResponse, emacEnrolResponse) = registerResponse
              emacEnrolResponse.status match {
                case CREATED => Redirect(controllers.routes.RegisterUserController.confirmation())
                case BAD_REQUEST | CONFLICT =>
                  val errMessage = formatEmacErrorMessage(DuplicateUserError)
                  Logger.warn(s"[RegisterUserController][registerUser] - allocation failed - organisation has already enrolled in EMAC")
                  Ok(views.html.global_error(errMessage._1, errMessage._2, errMessage._3))
                case FORBIDDEN =>
                  val errMessage = formatEmacErrorMessage(WrongRoleUserError)
                  Logger.warn(s"[RegisterUserController][registerUser] - allocation failed - wrong role for user enrolling in EMAC")
                  Ok(views.html.global_error(errMessage._1, errMessage._2, errMessage._3))
                case _ =>
                  Logger.warn(s"[RegisterUserController][registerUser] - allocation failed - no definite reason found")
                  throw new RuntimeException(Messages("ated.business-registration.error.other"))
              }

          }
        }
        }


  def confirmation = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
          Future.successful(Ok(views.html.registerUserConfirmation(Dates.formatDate(LocalDate.now()))))
  }

  def redirectToAted = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Redirect(ExternalUrls.atedStartPath)
  }


  private def formatEmacErrorMessage(str: String) =
    str match {
      case DuplicateUserError =>
        (Messages("ated.business-registration-error.duplicate.identifier.header"),
          Messages("ated.business-registration-error.duplicate.identifier.title"),
          Messages("ated.business-registration-error.duplicate.identifier.message"))
      case WrongRoleUserError =>
        (Messages("ated.business-registration-error.wrong.role.header"),
          Messages("ated.business-registration-error.wrong.role.title"),
          Messages("ated.business-registration-error.wrong.role.message"))
    }

}


object RegisterUserController extends RegisterUserController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val registerUserService = RegisterUserService
  // $COVERAGE-ON$
}
