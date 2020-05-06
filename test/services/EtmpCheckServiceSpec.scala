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

package services

import builders.AuthBuilder
import config.ApplicationConfig
import connectors.{AtedSubscriptionConnector, TaxEnrolmentsConnector}
import models.{Address, AtedSubscriptionAuthData, BusinessCustomerDetails, SelfHealSubscriptionResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EtmpCheckServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockTaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val mockAtedSubscriptionConnector = mock[AtedSubscriptionConnector]
  val mockRegisterUserService = mock[RegisterUserService]
  lazy val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val etmpCheckService = new EtmpCheckService(mockAtedSubscriptionConnector, mockTaxEnrolmentsConnector, mockRegisterUserService, mockAppConfig)

  "etmpCheckService" should {
    "validate business details successfully" when {
      "there is a business with regime etmp details" in {
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "user name")

        val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
          businessType = Some("corporate body"),
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))

        when(mockAtedSubscriptionConnector.checkEtmpBusinessPartnerExists(any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(SelfHealSubscriptionResponse("test"))))
        when(mockTaxEnrolmentsConnector.enrol(any(), any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(CREATED)))

        await(etmpCheckService.validateBusinessDetails(reviewDetails)) mustBe true
      }
    }

    "fail to validate business details successfully" when {
      "there is a business with regime etmp details but enrolling ES8 fails" in {
        implicit val user: AtedSubscriptionAuthData = AuthBuilder.createAgentAuthContext("userId", "user name")

        val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
          businessType = Some("corporate body"),
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))

        when(mockAtedSubscriptionConnector.checkEtmpBusinessPartnerExists(any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(SelfHealSubscriptionResponse("test"))))
        when(mockTaxEnrolmentsConnector.enrol(any(), any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))

        await(etmpCheckService.validateBusinessDetails(reviewDetails)) mustBe false
      }
    }
  }

}
