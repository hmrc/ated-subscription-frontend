/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.auth

import config.ApplicationConfig
import models.AtedSubscriptionAuthData
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthUtils._

import scala.concurrent.{ExecutionContext, Future}

trait AtedSubscriptionAuthHelpers {
  self: AuthFunctionality =>

  def agentAction(f: AtedSubscriptionAuthData => Future[Result])
                 (implicit req: Request[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    authoriseFor {
      implicit authContext =>
        if (isAgentUser) {
          f(authContext)
        } else if (isAssistant) {
          if (isAgent) {
            Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistantAgent))
          } else {
            Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistantOrg))
          }

        } else {
          Future.successful(Redirect(controllers.routes.ApplicationController.unauthorised))
        }
    }
  }

  def clientAction(f: AtedSubscriptionAuthData => Future[Result])
                  (implicit req: Request[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages, appConfig: ApplicationConfig): Future[Result] = {
    def redirectToSubscription(redirectName: String): Result = {
      val serviceRedirectUrl: String = appConfig.serviceRedirectUrl(redirectName)
      Redirect(serviceRedirectUrl)
    }

    authoriseFor {
      implicit authContext =>
        if (isAgentUser) {
          Future.successful(redirectToSubscription("microservice.services.business-customer.agentServiceRedirectUrl"))
        } else if (isAssistant) {
          if (isAgent) {
            Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistantAgent))
          } else {
            Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistantOrg))
          }
        } else {
          f(authContext)
        }
    }
  }
}
