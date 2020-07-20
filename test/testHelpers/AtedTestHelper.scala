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

package testHelpers

import config.ApplicationConfig
import connectors.AtedSubscriptionDataCacheConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc._
import services.RegisterUserService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AtedSubscriptionUtilsImpl

trait AtedTestHelper extends MockitoSugar  with GuiceOneServerPerSuite { this: TestSuite =>

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockRegisterUserService: RegisterUserService = mock[RegisterUserService]
  val mockAtedSubUtils: AtedSubscriptionUtilsImpl = mock[AtedSubscriptionUtilsImpl]
  val mockDataCacheConnector: AtedSubscriptionDataCacheConnector = mock[AtedSubscriptionDataCacheConnector]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockWSHttp: DefaultHttpClient = mock[DefaultHttpClient]


  when(mockAppConfig.agentEmailEditPath)
    .thenReturn("http://localhost:9959/mandate/agent/email/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
  when(mockAppConfig.clientDisplayNameEditPath)
    .thenReturn("http://localhost:9959/mandate/agent/client-display-name/ated?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
  when(mockAppConfig.businessNameAndAddressEditUrl)
    .thenReturn("http://localhost:9923/business-customer/register/non-uk-client/ATED/edit?redirectUrl=http://localhost:9933/ated-subscription/review-business-details")
  when(mockAppConfig.serviceRedirectUrl(ArgumentMatchers.eq("microservice.services.business-customer.serviceRedirectUrl")))
    .thenReturn("http://localhost:9923/business-customer/ATED?backLinkUrl=http://localhost:9933/ated-subscription/appoint-agent")
  when(mockAppConfig.serviceRedirectUrl(ArgumentMatchers.eq("microservice.services.business-customer.agentServiceRedirectUrl")))
    .thenReturn("http://localhost:9923/business-customer/agent/ATED?backLinkUrl=http://localhost:9933/ated-subscription/start-agent-subscription")

  when(mockAppConfig.signIn).thenReturn("http://localhost:9025/gg/sign-in?continue=http://localhost:9933/ated-subscription/start-subscription")
  when(mockAppConfig.agentAtedSummaryPath).thenReturn("http://localhost:9959/mandate/agent/summary")
  when(mockAppConfig.loginURL).thenReturn("http://localhost:9025/gg/sign-in")
  when(mockAppConfig.logoutPath).thenReturn("/ated/logout")
  when(mockAppConfig.atedStartPath).thenReturn("/ated/home")
  when(mockAppConfig.cancelRedirectUrl).thenReturn("https://www.gov.uk/")
  when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)
  when(mockAppConfig.analyticsToken).thenReturn(Some("UA-123456"))
  when(mockAppConfig.atedSubsUtils).thenReturn(new AtedSubscriptionUtilsImpl(app.environment))

  lazy val mockMCC: DefaultMessagesControllerComponents = app.injector.instanceOf[DefaultMessagesControllerComponents]
}
