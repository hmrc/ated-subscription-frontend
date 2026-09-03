/*
 * Copyright 2026 HM Revenue & Customs
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
import connectors.AtedSubscriptionDataCacheConnector
import controllers.auth.{AtedSubscriptionAuthHelpers, AuthFunctionality}
import forms.AtedForms.*

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject()(mcc: MessagesControllerComponents,
                                       val authConnector: DefaultAuthConnector,
                                       templateSubscription: views.html.subscription,
                                       templateAppointAgent: views.html.appointAgent,
                                       templateAgentSubscription: views.html.agentSubscription,
                                       beforeRegisterAgentPage: views.html.beforeRegisterAgent,
                                       beforeRegisteringForATEDPage: views.html.beforeRegisteringForATED,
                                       dataCacheConnector: AtedSubscriptionDataCacheConnector)
                                       (using val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with AtedSubscriptionAuthHelpers with AuthFunctionality {

  given ec: ExecutionContext = mcc.executionContext

  def subscribe: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit data =>
      if (AuthUtils.isAgent) {
        Future.successful(
          Redirect(controllers.routes.SubscriptionController.subscribeAgent)
        )
      } else {
        dataCacheConnector.fetchAreYouAnAgent.map { cachedData =>
          val form = cachedData.fold(areYouAnAgentForm)(areYouAnAgentForm.fill)

          Ok(templateSubscription(form))
        }
      }
    }
  }

  def appoint: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit data =>
      dataCacheConnector.fetchAppointAgent.map { cachedData =>
        val form = cachedData.fold(appointAgentForm)(appointAgentForm.fill)

        Ok(
          templateAppointAgent(
            form,
            Some(controllers.routes.SubscriptionController.subscribe.url)
          )
        )
      }
    }
  }

  def subscribeAgent: Action[AnyContent] = Action.async { implicit req =>
    agentAction { implicit user =>
      Future.successful(Ok(templateAgentSubscription()))
    }
  }

  def continue: Action[AnyContent] = Action.async { implicit req =>
    clientAction { implicit user =>
      areYouAnAgentForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(templateSubscription(formWithErrors))),
        answers => {
          dataCacheConnector.saveAreYouAnAgent(answers).map{
            _ => Redirect(controllers.routes.SubscriptionController.appoint)
          }
        }
      )
    }
  }
  
  def beforeRegisterGuidance: Action[AnyContent] = Action.async { implicit req =>
    clientAction { implicit user =>
      appointAgentForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              templateAppointAgent(
                formWithErrors,
                Some(controllers.routes.SubscriptionController.subscribe.url)
              )
            )
          ),
        agentStatus =>
          dataCacheConnector.saveAppointAgent(agentStatus).map { _ =>
            agentStatus.isAgent match {
              case Some(true) =>
                Redirect(
                  controllers.routes.SubscriptionController.showBeforeRegisteringAgentPage
                )

              case _ =>
                Redirect(
                  controllers.routes.SubscriptionController.showBeforeRegisteringATEDPage
                )
            }
          }
      )
    }
  }

  def showBeforeRegisteringAgentPage : Action[AnyContent] = Action.async { implicit request =>
    clientAction { implicit user =>
      Future.successful(Ok(beforeRegisterAgentPage(Some(controllers.routes.SubscriptionController.appoint.url))))
    }
  }

  def showBeforeRegisteringATEDPage : Action[AnyContent] = Action.async { implicit request =>
    clientAction { implicit user =>
      Future.successful(Ok(beforeRegisteringForATEDPage(Some(controllers.routes.SubscriptionController.appoint.url))))
    }
  }

}
