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
import controllers.auth.{AtedSubscriptionAuthHelpers, AuthFunctionality}
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AuthUtils
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject()(mcc: MessagesControllerComponents,
                                       val authConnector: DefaultAuthConnector,
                                       templateSubscription: views.html.subscription,
                                       templateAppointAgent: views.html.appointAgent,
                                       templateAgentSubscription: views.html.agentSubscription,
                                       implicit val appConfig: ApplicationConfig
                                      ) extends FrontendController(mcc) with AtedSubscriptionAuthHelpers with AuthFunctionality with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  def subscribe: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit data =>
      if (AuthUtils.isAgent) {
        Future.successful(Redirect(controllers.routes.SubscriptionController.subscribeAgent))
      } else {
        Future.successful(Ok(templateSubscription(areYouAnAgentForm)))
      }
    }
  }

  def appoint: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        Future.successful(
          Ok(templateAppointAgent(appointAgentForm, Some(controllers.routes.SubscriptionController.subscribe.url)))
        )
      }
  }

  def subscribeAgent: Action[AnyContent] = Action.async { implicit req =>
    agentAction { implicit user =>
      Future.successful(Ok(templateAgentSubscription()))
    }
  }

  def continue: Action[AnyContent] = Action.async { implicit req =>
    clientAction { implicit user =>
      areYouAnAgentForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(templateSubscription(formWithErrors))),
        _ => Future.successful(Redirect(controllers.routes.SubscriptionController.appoint))
      )
    }
  }

  def register: Action[AnyContent] = Action.async { implicit req =>
    clientAction { implicit user =>
      appointAgentForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(templateAppointAgent(formWithErrors,
          Some(controllers.routes.SubscriptionController.subscribe.url)))),
        _ => redirectToSubscription("microservice.services.business-customer.serviceRedirectUrl")
      )
    }
  }

  private def redirectToSubscription(redirectName: String): Future[Result] = {
    Future.successful(Redirect(appConfig.serviceRedirectUrl(redirectName)))
  }

}