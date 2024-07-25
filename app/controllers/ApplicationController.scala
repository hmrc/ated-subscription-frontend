/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, DiscardingCookie, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class ApplicationController @Inject()(mcc: MessagesControllerComponents,
                                      dataCacheConnector: AtedSubscriptionDataCacheConnector,
                                      val authConnector: DefaultAuthConnector,
                                      templateUnauthorised: views.html.unauthorised,
                                      templateUnauthorisedAssistantOrg: views.html.unauthorisedAssistantOrg,
                                      templateUnauthorisedAssistantAgent: views.html.unauthorisedAssistantAgent,
                                      implicit val appConfig: ApplicationConfig
                                     ) extends FrontendController(mcc) with AuthFunctionality with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def unauthorised: Action[AnyContent] = Action { implicit request =>
    Ok(templateUnauthorised())
  }

  def cancel: Action[AnyContent] = Action {
    Redirect(appConfig.cancelRedirectUrl)
  }

  def logout: Action[AnyContent] = Action {
    Redirect(appConfig.logoutPath)
  }

  def keepAlive: Action[AnyContent] = Action {
    Ok("OK")
  }
  def redirectToAtedStart: Action[AnyContent] = Action {
    Redirect(appConfig.atedStartPath).discardingCookies(DiscardingCookie("mdtp"))
  }
  def redirectToLogout: Action[AnyContent] = Action {
      Redirect(appConfig.logoutPath)
  }
  def redirectToGuidance: Action[AnyContent] = Action {
      Redirect(appConfig.guidanceUrl).withNewSession
  }
  def unauthorisedAssistantOrg: Action[AnyContent] = Action {
    implicit request => Ok(templateUnauthorisedAssistantOrg())
  }
  def unauthorisedAssistantAgent: Action[AnyContent] = Action {
    implicit request => Ok(templateUnauthorisedAssistantAgent())
  }
  def clearCache: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { _ =>
      dataCacheConnector.clearCache.map { _ =>
        logger.info("session has been cleared")
        Ok
      }.recover{
        case t: Throwable =>
          logger.error(s"session has not been cleared for ATED_SUBSCRIPTION, $t")
          InternalServerError
      }
    }
  }
}
