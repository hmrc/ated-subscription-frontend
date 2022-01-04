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

package services

import connectors.AtedSubscriptionDataCacheConnector
import models.Address
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CorrespondenceAddressServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockDataCacheConnector: AtedSubscriptionDataCacheConnector = mock[AtedSubscriptionDataCacheConnector]

  val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")

  val testCorrespondenceAddressService: CorrespondenceAddressService = new CorrespondenceAddressService(mockDataCacheConnector)

  override def beforeEach(): Unit = {
    reset(mockDataCacheConnector)
  }

  "CorrespondenceAddressService" must {

    "saveCorrespondenceAddress" must {
      "save correspondence address in Keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
        val result = testCorrespondenceAddressService.saveCorrespondenceAddress(testAddress)
        await(result).get.toString must be(testAddress.toString)
        verify(mockDataCacheConnector, times(1)).saveCorrespondenceAddress(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
    "fetchCorrespondenceAddress" must {
      "return correspondence address, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchCorrespondenceAddress).thenReturn(Future.successful(Some(testAddress)))
        val result = testCorrespondenceAddressService.fetchCorrespondenceAddress
        await(result) must be(Some(testAddress))
        verify(mockDataCacheConnector, times(1)).fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
      "return None, if not found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchCorrespondenceAddress).thenReturn(Future.successful(None))
        val result = testCorrespondenceAddressService.fetchCorrespondenceAddress
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }

}
