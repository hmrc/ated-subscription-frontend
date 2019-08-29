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
import connectors.AgentClientMandateFrontendConnector
import controllers.auth.AuthFunctionality
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{MandateService, RegisterUserService}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DeclarationController extends FrontendController with AuthFunctionality {



  def registerEmacUserService: RegisterUserService

  def mandateService: MandateService

  def agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(views.html.nonUKReg.declaration(getBackLink)))
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        agentClientFrontendMandateConnector.getOldMandateDetails flatMap {
          case Some(mandateFound) =>
            mandateService.updateMandateForNonUK(mandateFound.atedRefNumber, mandateFound.mandateId) flatMap { mandateResponse =>
              Future.successful(Redirect(routes.ConfirmationController.view()))
            }
          case None =>
            registerEmacUserService.subscribeAted(isNonUKClientRegisteredByAgent = true) flatMap { response =>
              val (etmpSubscriptionResponse, emacEnrolResponse) = response
              val atedRefNo = etmpSubscriptionResponse.atedRefNumber.getOrElse(throw new RuntimeException("ated reference number not found"))
              mandateService.createMandateForNonUK(atedRefNo) flatMap { mandateResponse =>
                Future.successful(Redirect(routes.ConfirmationController.view()))
              }
            }
        }
      }
  }

  def getBackLink: Some[String] = {
    Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails().url)
  }
}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  val authConnector = AuthClientConnector
  val registerEmacUserService: RegisterUserService = RegisterUserService
  val mandateService: MandateService = MandateService
  val agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector = AgentClientMandateFrontendConnector
  // $COVERAGE-ON$
}
