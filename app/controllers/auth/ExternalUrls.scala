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
import play.api.Play.current
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {

  val companyAuthHost = s"${Play.configuration.getString("microservice.services.auth.company-auth.host").getOrElse("")}"
  val loginCallback = Play.configuration.getString("microservice.services.auth.login-callback.url").getOrElse("/ated-subscription/start")
  val loginPath = s"${Play.configuration.getString("microservice.services.auth.login-path").getOrElse("")}"
  val signIn = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback"
  val loginURL = s"$companyAuthHost/gg/$loginPath"
  val signOut = s"$companyAuthHost/gg/sign-out"

  val logoutPath = Play.configuration.getString("microservice.services.ated-frontend.logoutUrl").getOrElse("/ated/logout")
  val atedStartPath = Play.configuration.getString("microservice.services.ated-frontend.atedStartRedirectUrl").getOrElse("/ated/home")

  val reviewDetailsPath = Play.configuration.getString("microservice.services.business-customer.reviewDetailsUrl")
    .getOrElse("/business-customer/review-details/ATED")

  val agentAtedSummaryPath = Play.configuration.getString("microservice.services.agent-client-mandate-frontend.agentAtedSummaryUrl").getOrElse("/mandate/agent/summary")
  val clientDisplayNameEditPath = Play.configuration.getString("microservice.services.agent-client-mandate-frontend.clientDisplayNameEditUrl")
    .getOrElse("/mandate/agent/client-display-name/edit")
  val agentEmailEditPath = Play.configuration.getString("microservice.services.agent-client-mandate-frontend.agentEmailEditUrl")
    .getOrElse("/mandate/agent/email/edit")

  val businessNameAndAddressEditUrl = Play.configuration.getString("microservice.services.business-customer.businessNameAndAddressEditUrl")
    .getOrElse("/business-customer/non-uk-client/ATED/edit?redirectUrl=/ated-subscription/review-business-details")

  val overseasTaxReferenceEditUrl = Play.configuration.getString("microservice.services.business-customer.overseasTaxReferenceEditUrl")
    .getOrElse("/business-customer/register/non-uk-client/edit-overseas-company/ATED/true?redirectUrl=/ated-subscription/review-business-details")

  val backToBusinessCustomerUrl = Play.configuration.getString("microservice.services.business-customer.backLinkUrl")
    .getOrElse("/business-customer/back-link/ATED")
}
