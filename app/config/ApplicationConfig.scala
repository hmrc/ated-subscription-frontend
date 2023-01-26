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

package config

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AtedSubscriptionUtils

class ApplicationConfig @Inject()(val servicesConfig: ServicesConfig,
                                  val atedSubsUtils: AtedSubscriptionUtils) {

  lazy val appName: String = servicesConfig.getConfString("appName", "ated-subscription-frontend")
  lazy val serviceUrlACMFrontend: String = servicesConfig.baseUrl("agent-client-mandate-frontend")
  lazy val serviceUrlACM: String = servicesConfig.baseUrl("agent-client-mandate")
  lazy val serviceUrlAted: String = servicesConfig.baseUrl("ated")
  lazy val serviceUrlAtedSub: String = servicesConfig.baseUrl("ated-subscription")
  lazy val serviceUrlGG: String = servicesConfig.baseUrl("government-gateway")
  lazy val serviceUrlTaxEnrol: String = servicesConfig.baseUrl("tax-enrolments")
  lazy val serviceUrlBC: String = servicesConfig.baseUrl("business-customer-frontend")

  private val contactFormServiceIdentifier: String = "ATED"
  private val contactHost: String = servicesConfig.getConfString("contact-frontend.host", "")
  def betaFeedbackUrl(returnUri: String): String = servicesConfig.getConfString("beta-feedback-url", defaultBetaFeedbackUrl) + "?return=" + returnUri

  lazy val defaultBetaFeedbackUrl: String = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl: String = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val defaultTimeoutSeconds: Int = servicesConfig.getInt("defaultTimeoutSeconds")
  lazy val timeoutCountdown: Int = servicesConfig.getInt("timeoutCountdown")


  //ExternalUrls
  def serviceRedirectUrl(redirectName: String): String = servicesConfig.getString(redirectName)

  lazy val cancelRedirectUrl: String = servicesConfig.getConfString("cancelRedirectUrl", "https://www.gov.uk/")
  lazy val companyAuthHost: String = s"${servicesConfig.getConfString("auth.company-auth.host", "")}"
  lazy val loginCallback: String = servicesConfig.getConfString("auth.login-callback.url", "/ated-subscription/start")
  lazy val loginPath: String = s"${servicesConfig.getConfString("auth.login-path", "")}"
  lazy val signIn: String = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback"
  lazy val loginURL: String = s"$companyAuthHost/gg/$loginPath"
  lazy val signOut: String = s"$companyAuthHost/gg/sign-out"
  lazy val guidanceUrl: String = servicesConfig.getString("guidanceUrl")

  lazy val helpdeskUrl: String = servicesConfig.getString("helpdeskUrl")
  lazy val agentAccountPortalUrl: String = servicesConfig.getString("agentAccountPortalUrl")

  lazy val logoutPath: String = servicesConfig.getConfString("ated-frontend.logoutUrl", "/ated/logout")
  lazy val atedStartPath: String = servicesConfig.getConfString("ated-frontend.atedStartRedirectUrl", "/ated/home")

  lazy val reviewDetailsPath: String = servicesConfig.getConfString(
    "business-customer.reviewDetailsUrl","/business-customer/review-details/ATED")
  lazy val nrlPath: String = servicesConfig.getConfString(
    "business-customer.nrlUrl","/business-customer/nrl/ATED")
  lazy val agentAtedSummaryPath: String = servicesConfig.getConfString(
    "agent-client-mandate-frontend.agentAtedSummaryUrl", "/mandate/agent/summary")
  lazy val clientDisplayNameEditPath: String = servicesConfig.getConfString(
    "agent-client-mandate-frontend.clientDisplayNameEditUrl", "/mandate/agent/client-display-name/edit")
  lazy val agentEmailEditPath: String = servicesConfig.getConfString(
    "agent-client-mandate-frontend.agentEmailEditUrl", "/mandate/agent/email/edit")

  lazy val businessNameAndAddressEditUrl: String = servicesConfig.getConfString(
    "business-customer.businessNameAndAddressEditUrl",
    "/business-customer/non-uk-client/ATED/edit?redirectUrl=/ated-subscription/review-business-details")

  lazy val overseasTaxReferenceEditUrl: String = servicesConfig.getConfString("business-customer.overseasTaxReferenceEditUrl",
    "/business-customer/register/non-uk-client/edit-overseas-company/ATED/true?redirectUrl=/ated-subscription/review-business-details")

  lazy val backToBusinessCustomerUrl: String = servicesConfig.getConfString("business-customer.backLinkUrl",
    "/business-customer/back-link/ATED")

  lazy val toBusinessAccountUrl: String = servicesConfig.getConfString("business-tax-account.serviceRedirectUrl", "/business-account")

}
