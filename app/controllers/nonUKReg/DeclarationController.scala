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

package controllers.nonUKReg

import config.ApplicationConfig
import connectors.AgentClientMandateFrontendConnector
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{MandateService, RegisterUserService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(mcc: MessagesControllerComponents,
                                      registerUserService: RegisterUserService,
                                      mandateService: MandateService,
                                      agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector,
                                      val authConnector: DefaultAuthConnector,
                                      template: views.html.nonUKReg.declaration,
                                      implicit val appConfig: ApplicationConfig
                                     ) extends FrontendController(mcc)  with AuthFunctionality {
  implicit val ec: ExecutionContext = mcc.executionContext

  def view: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(Ok(template(getBackLink)))
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        agentClientFrontendMandateConnector.getOldMandateDetails flatMap {
          case Some(mandateFound) =>
            mandateService.updateMandateForNonUK(mandateFound.atedRefNumber, mandateFound.mandateId) flatMap { _ =>
              Future.successful(Redirect(routes.ConfirmationController.view))
            }
          case None =>
            registerUserService.subscribeAted(isNonUKClientRegisteredByAgent = true) flatMap { etmpSubscriptionResponse =>
              val atedRefNo = etmpSubscriptionResponse.atedRefNumber.getOrElse(throw new RuntimeException("ated reference number not found"))
              mandateService.createMandateForNonUK(atedRefNo) flatMap { _ =>
                Future.successful(Redirect(routes.ConfirmationController.view))
              }
            }
        }
      }
  }

  def getBackLink: Some[String] = {
    Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
  }
}
