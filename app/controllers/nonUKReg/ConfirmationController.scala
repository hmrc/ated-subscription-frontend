/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import org.joda.time.LocalDate
import services.RegisteredBusinessService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Dates
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait ConfirmationController extends FrontendController with Actions {

  def registeredBusinessService: RegisteredBusinessService

  def view = AuthorisedFor(AtedSubscriptionRegime, GGConfidence).async {
    implicit user => implicit request =>
      registeredBusinessService.getReviewBusinessDetails.map(_.businessName) map { name =>
        Ok(views.html.nonUKReg.confirmation(name, Dates.formatDate(LocalDate.now())))
      }
  }

  def continue = AuthorisedFor(AtedSubscriptionRegime, GGConfidence) {
    implicit user => implicit request => Redirect(ExternalUrls.agentAtedSummaryPath)
  }

}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val registeredBusinessService: RegisteredBusinessService = RegisteredBusinessService
  // $COVERAGE-ON$
}
