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
import controllers.auth.AtedSubscriptionRegime
import services.{MandateService, RegisterUserService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DeclarationController extends FrontendController with Actions {

  def registerUserService: RegisterUserService

  def mandateService: MandateService

  def view = AuthorisedFor(AtedSubscriptionRegime, GGConfidence) {
    implicit user => implicit request => Ok(views.html.nonUKReg.declaration(getBackLink))
  }

  def submit = AuthorisedFor(AtedSubscriptionRegime, GGConfidence).async {
    implicit user => implicit request =>
      registerUserService.subscribeAted(isNonUKClientRegisteredByAgent = true) flatMap { response =>
        val atedRefNo = response._1.atedRefNumber.getOrElse(throw new RuntimeException("ated reference number not found"))
        mandateService.createMandateForNonUK(atedRefNo) flatMap { mandateResponse =>
          Future.successful(Redirect(routes.ConfirmationController.view()))
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
  val mandateService: MandateService = MandateService
  // $COVERAGE-ON$
}