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

import connectors.AtedSubscriptionDataCacheConnector
import models.{ContactDetails, ContactDetailsEmail, PreviousSubmittedForm}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OverseasServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockDataCacheConnector: AtedSubscriptionDataCacheConnector = mock[AtedSubscriptionDataCacheConnector]
  val testOverseasCompanyService: OverseasCompanyService = new OverseasCompanyService(mockDataCacheConnector)

  val testPreviouslySubmitted = PreviousSubmittedForm(Some(true))

  override def beforeEach: Unit = {
    reset(mockDataCacheConnector)
  }

  "OverseasService" must {
    "savePreviouslySubmitted" must {
      "save previously submitted into keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.savePreviouslySubmitted(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testPreviouslySubmitted)))
        val result = testOverseasCompanyService.savePreviouslySubmitted(testPreviouslySubmitted)
        await(result).get.toString must be(testPreviouslySubmitted.toString)
        verify(mockDataCacheConnector, times(1)).savePreviouslySubmitted(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
    "fetchPreviouslySubmitted" must {
      "return previously submitted, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchPreviouslySubmittedForSession).thenReturn(Future.successful(Some(testPreviouslySubmitted)))
        val result = testOverseasCompanyService.fetchPreviouslySubmitted
        await(result) must be(Some(testPreviouslySubmitted))
        verify(mockDataCacheConnector, times(1)).fetchPreviouslySubmittedForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
      "return None, if not found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchPreviouslySubmittedForSession).thenReturn(Future.successful(None))
        val result = testOverseasCompanyService.fetchPreviouslySubmitted
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchPreviouslySubmittedForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }

}
