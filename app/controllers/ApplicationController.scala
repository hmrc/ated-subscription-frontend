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
import connectors.AtedSubscriptionDataCacheConnector
import controllers.auth.AuthFunctionality
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, DiscardingCookie, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class ApplicationController @Inject()(mcc: MessagesControllerComponents,
                                      dataCacheConnector: AtedSubscriptionDataCacheConnector,
                                      val authConnector: DefaultAuthConnector,
                                      implicit val appConfig: ApplicationConfig
                                     ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def unauthorised(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.unauthorised())
  }

  def cancel(): Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.cancelRedirectUrl)
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.logoutPath)
  }

  def keepAlive: Action[AnyContent] = Action { implicit request =>
    Ok("OK")
  }

  def redirectToAtedStart: Action[AnyContent] = Action {
    implicit request =>
      Redirect(appConfig.atedStartPath).discardingCookies(DiscardingCookie("mdtp"))
  }

  def redirectToLogout: Action[AnyContent] = Action {
    implicit request =>
      Redirect(appConfig.logoutPath)
  }

  def redirectToGuidance: Action[AnyContent] = Action {
    implicit request =>
      Redirect(appConfig.guidanceUrl).withNewSession
  }

  def unauthorisedAssistantOrg: Action[AnyContent] = Action {
    implicit request => Ok(views.html.unauthorisedAssistantOrg())
  }

  def unauthorisedAssistantAgent: Action[AnyContent] = Action {
    implicit request => Ok(views.html.unauthorisedAssistantAgent())
  }

  def clearCache: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit auth =>
      dataCacheConnector.clearCache.map { x =>
        x.status match {
          case OK | NO_CONTENT =>
            Logger.info("session has been cleared")
            Ok
          case errorStatus =>
            Logger.error(s"session has not been cleared for ATED_SUBSCRIPTION. Status: $errorStatus, Error: ${x.body}")
            InternalServerError
        }
      }
    }
  }
}
