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
import controllers.auth.{AtedSubscriptionAuthHelpers, AtedSubscriptionRegime}
import forms.AtedForms._
import play.api.Mode.Mode
import play.api.i18n.Messages.Implicits._
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.AuthUtils

trait SubscriptionController extends FrontendController with AtedSubscriptionAuthHelpers {

  import play.api.Play.current

  def subscribe = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      AuthUtils.isAgent match {
        case true => Redirect(controllers.routes.SubscriptionController.subscribeAgent())
        case false => Ok(views.html.subscription(areYouAnAgentForm))
      }
  }

  def appoint = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      Ok(views.html.appointAgent(appointAgentForm, Some(controllers.routes.SubscriptionController.subscribe().url)))
  }

  def subscribeAgent = AgentAction(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      Ok(views.html.agentSubscription())
  }

  def continue = ClientAction(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      areYouAnAgentForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.subscription(formWithErrors)),
        areYouAnAgent => Redirect(controllers.routes.SubscriptionController.appoint())
      )
  }

  def register = ClientAction(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence) {
    implicit user => implicit request =>
      appointAgentForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.appointAgent(formWithErrors)),
        appointAgent => redirectToSubscription("microservice.services.business-customer.serviceRedirectUrl")
      )
  }

  private def redirectToSubscription(redirectName: String) = {
    val serviceRedirectUrl: String = Play.configuration.getString(redirectName).getOrElse("/business-customer/ATED")
    Redirect(serviceRedirectUrl)
  }

}

object SubscriptionController extends SubscriptionController {
  val authConnector = FrontendAuthConnector

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
