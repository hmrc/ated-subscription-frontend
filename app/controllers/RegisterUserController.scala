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
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegisterUserService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates
import utils.AuthUtils._

import scala.concurrent.{ExecutionContext, Future}

class RegisterUserController @Inject()(mcc: MessagesControllerComponents,
                                       registerUserService: RegisterUserService,
                                       val authConnector: DefaultAuthConnector,
                                       implicit val appConfig: ApplicationConfig
                                      ) extends FrontendController(mcc) with AuthFunctionality {


  implicit val ec: ExecutionContext = mcc.executionContext
  private val WrongRoleUserError = "wrong role user error"
  private val GenericError = "generic user error"

  def registerUser: Action[AnyContent] = Action.async { implicit request =>
     authoriseFor { implicit data =>
        if (isAgent) Future.successful(Redirect(controllers.nonUKReg.routes.DeclarationController.view()))
        else {
          registerUserService.subscribeAted() map { registerResponse =>
            val (_, emacEnrolResponse) = registerResponse
            emacEnrolResponse.status match {
              case CREATED => Redirect(controllers.routes.RegisterUserController.confirmation())
              case CONFLICT =>
                Logger.warn(s"[RegisterUserController][registerUser] - allocation failed - organisation has already enrolled in EMAC")
                Ok(views.html.alreadyRegistered())
              case FORBIDDEN =>
                val (pageTitle, heading, message) = formatEmacErrorMessage(WrongRoleUserError)
                Logger.warn(s"[RegisterUserController][registerUser] - allocation failed - wrong role for user enrolling in EMAC")
                Ok(views.html.global_error(pageTitle, heading, message))
              case _ =>
                val (pageTitle, heading, message) = formatEmacErrorMessage(GenericError)
                Logger.warn("[RegisterUserController][registerUser] - allocation failed - no definite reason found")
                Ok(views.html.global_error(pageTitle, heading, message))
            }
          }
        }
      }
  }

  def confirmation: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(views.html.registerUserConfirmation(Dates.formatDate(LocalDate.now()))))
      }
  }

  def redirectToAted: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(appConfig.atedStartPath))
      }
  }


  private def formatEmacErrorMessage(str: String): (String, String, String) =
    str match {
      case WrongRoleUserError =>
        ("ated.business-registration-error.wrong.role.header",
          "ated.business-registration-error.wrong.role.title",
          "ated.business-registration-error.wrong.role.message")
      case GenericError =>
        ("ated.business-registration.generic.error.header",
          "ated.business-registration.generic.error.title",
          "ated.business-registration.generic.error.message")
    }

}