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

package controllers.auth

import models.AtedSubscriptionAuthData
import play.api.Play
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode
import utils.AuthUtils._

import scala.concurrent.{ExecutionContext, Future}

trait AtedSubscriptionAuthHelpers extends RunMode {
  self: AuthFunctionality =>

  import play.api.Play.current

  def agentAction(f: AtedSubscriptionAuthData => Future[Result])
                 (implicit req: Request[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    authoriseFor {
      implicit authContext =>
        if (isAgentAdmin) {
          f(authContext)
        } else if (isAgentAssistant) {
          Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistant()))
        } else {
          Future.successful(Redirect(controllers.routes.ApplicationController.unauthorised()))
        }
    }
  }

  def clientAction(f: AtedSubscriptionAuthData => Future[Result])
                  (implicit req: Request[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    def redirectToSubscription(redirectName: String): Result = {
      val serviceRedirectUrl: String = Play.configuration.getString(redirectName).getOrElse("/business-customer/ATED")
      Redirect(serviceRedirectUrl)
    }

    authoriseFor {
      implicit authContext =>
        if (isAgentAdmin) {
          Future.successful(redirectToSubscription("microservice.services.business-customer.agentServiceRedirectUrl"))
        } else if (isAgentAssistant) {
          Future.successful(Redirect(controllers.routes.ApplicationController.unauthorisedAssistant()))
        } else {
          f(authContext)
        }
    }
  }

}
