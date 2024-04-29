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
import controllers.auth.AuthFunctionality

import javax.inject.Inject
import play.api.Logging
import play.api.i18n.{Messages, MessagesProvider}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Result}
import services.RegisterUserService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AuthUtils._
import utils.Dates

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class RegisterUserController @Inject()(mcc: MessagesControllerComponents,
                                       registerUserService: RegisterUserService,
                                       val authConnector: DefaultAuthConnector,
                                       templateAlreadyRegistered: views.html.alreadyRegistered,
                                       templateRegisterUserConfirmation: views.html.registerUserConfirmation,
                                       templateError: views.html.global_error,
                                       implicit val appConfig: ApplicationConfig
                                      ) extends FrontendController(mcc) with AuthFunctionality with Logging {


  implicit val ec: ExecutionContext = mcc.executionContext
  private val WrongRoleUserError = "-error.wrong.role"
  private val GenericError = ".generic.error"

  def subscribeAndEnrolForAted: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit data =>
      if (isAgent) {
        Future.successful(Redirect(controllers.nonUKReg.routes.DeclarationController.view))
      } else {
        for {
          subscribeAtedSuccess <- registerUserService.subscribeAted()
          enrolAtedResponse <- registerUserService.enrolAted(subscribeAtedSuccess)
          action <- handleEnrolResponse(enrolAtedResponse)
        } yield {
          action
        }
      }
    }
  }

  def handleEnrolResponse(enrolAtedResponse: HttpResponse)(implicit request: MessagesRequest[AnyContent]): Future[Result] = {
    enrolAtedResponse.status match {
      case CREATED => Future.successful(Redirect(controllers.routes.RegisterUserController.confirmation))
      case CONFLICT =>
        logger.warn(s"[RegisterUserController][registerUser] - allocation failed - organisation has already enrolled in EMAC")
        Future.successful(Conflict(templateAlreadyRegistered()))
      case FORBIDDEN =>
        val (pageTitle, heading, message) = formatEmacErrorMessage(WrongRoleUserError)
        logger.warn(s"[RegisterUserController][registerUser] - allocation failed - wrong role for user enrolling in EMAC")
        Future.successful(Forbidden(templateError(pageTitle, heading, message)))
      case _ =>
        val (pageTitle, heading, message) = formatEmacErrorMessage(GenericError)
        logger.warn("[RegisterUserController][registerUser] - allocation failed - no definite reason found")
        Future.successful(InternalServerError(templateError(pageTitle, heading, message)))
    }
  }

  def confirmation: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(templateRegisterUserConfirmation(Dates.formatDate(ZonedDateTime.now(ZoneId.of("UTC"))))))
      }
  }

  def redirectToAted: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Redirect(appConfig.atedStartPath))
      }
  }

  private def formatEmacErrorMessage(key: String)(implicit messagesProvider: MessagesProvider): (String, String, String) =
    (Messages(s"ated.business-registration$key.header"),
      Messages(s"ated.business-registration$key.title"),
      Messages(s"ated.business-registration$key.message"))
}
