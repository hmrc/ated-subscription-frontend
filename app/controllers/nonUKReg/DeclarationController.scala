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

package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.AgentClientMandateFrontendConnector
import controllers.auth.AtedSubscriptionRegime
import services.{MandateService, RegisterEmacUserService, RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.RegisterUserService.runModeConfiguration
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DeclarationController extends FrontendController with Actions with RunMode{

  val isEmacFeatureToggle: Boolean

  def registerUserService: RegisterUserService

  def registerEmacUserService: RegisterEmacUserService

  def mandateService: MandateService

  def agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector

  def view = AuthorisedFor(AtedSubscriptionRegime, GGConfidence) {
    implicit user => implicit request => Ok(views.html.nonUKReg.declaration(getBackLink))
  }

  def submit = AuthorisedFor(AtedSubscriptionRegime, GGConfidence).async {
    implicit user =>
      implicit request =>
        agentClientFrontendMandateConnector.getOldMandateDetails flatMap {
            case Some(mandateFound) =>
                mandateService.updateMandateForNonUK(mandateFound.atedRefNumber, mandateFound.mandateId) flatMap { mandateResponse =>
                Future.successful(Redirect(routes.ConfirmationController.view()))
              }
            case None => {
              if(isEmacFeatureToggle){
                  registerEmacUserService.subscribeAted(isNonUKClientRegisteredByAgent = true) flatMap { response =>
                  val atedRefNo = response._1.atedRefNumber.getOrElse(throw new RuntimeException("ated reference number not found"))
                    mandateService.createMandateForNonUK(atedRefNo) flatMap { mandateResponse =>
                    Future.successful(Redirect(routes.ConfirmationController.view()))
                  }
                }
              }
              else{
               registerUserService.subscribeAted(isNonUKClientRegisteredByAgent = true) flatMap { response =>
                val atedRefNo = response._1.atedRefNumber.getOrElse(throw new RuntimeException("ated reference number not found"))
                mandateService.createMandateForNonUK(atedRefNo) flatMap { mandateResponse =>
                  Future.successful(Redirect(routes.ConfirmationController.view()))
                }
              }
              }
            }
        }
  }

  def getBackLink() = {
    Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
  }
}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val registerUserService: RegisterUserService = RegisterUserService
  val registerEmacUserService: RegisterEmacUserService = RegisterEmacUserService
  val mandateService: MandateService = MandateService
  val agentClientFrontendMandateConnector: AgentClientMandateFrontendConnector = AgentClientMandateFrontendConnector
  val isEmacFeatureToggle : Boolean = runModeConfiguration.getBoolean("emacsFeatureToggle").getOrElse(true)
  // $COVERAGE-ON$
}
