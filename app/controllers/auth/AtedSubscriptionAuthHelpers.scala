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

package controllers.auth

import play.api.Play
import play.api.mvc.{Action, Result, AnyContent, Request}
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.{AuthContext, PageVisibilityPredicate, TaxRegime, Actions}
import utils.AuthUtils._

//scalastyle:off
trait AtedSubscriptionAuthHelpers extends Actions with RunMode {

  import play.api.Play.current

  def AgentAction(taxRegime: TaxRegime, pageVisibility: PageVisibilityPredicate)(f: AuthContext => Request[AnyContent] => Result): Action[AnyContent] = {
    AuthorisedFor(taxRegime = taxRegime, pageVisibility = pageVisibility) {
      implicit authContext => implicit request =>
        if (isAgentAdmin) f(authContext)(request)
        else if (isAgentAssistant) Redirect(controllers.routes.ApplicationController.unauthorisedAssistant())
        else Redirect(controllers.routes.ApplicationController.unauthorised())
    }
  }

  def ClientAction(taxRegime: TaxRegime, pageVisibility: PageVisibilityPredicate)(f: AuthContext => Request[AnyContent] => Result): Action[AnyContent] = {
    def redirectToSubscription(redirectName: String) = {
      val serviceRedirectUrl: String = Play.configuration.getString(redirectName).getOrElse("/business-customer/ATED")
      Redirect(serviceRedirectUrl)
    }

    AuthorisedFor(taxRegime = taxRegime, pageVisibility = pageVisibility) {
      implicit authContext => implicit request =>
        if (isAgentAdmin) redirectToSubscription("microservice.services.business-customer.agentServiceRedirectUrl")
        else if (isAgentAssistant) Redirect(controllers.routes.ApplicationController.unauthorisedAssistant())
        else f(authContext)(request)
    }
  }

}
