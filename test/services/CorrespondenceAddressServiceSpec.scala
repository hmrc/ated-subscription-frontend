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

package services

import connectors.{AtedSubscriptionDataCacheConnector, DataCacheConnector}
import models.Address
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CorrespondenceAddressServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")

  object TestCorrespondenceAddressService extends CorrespondenceAddressService {
    val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockDataCacheConnector)
  }

  "CorrespondenceAddressService" must {
    "use the correct DataCacheconnector" in {
      CorrespondenceAddressService.dataCacheConnector must be(AtedSubscriptionDataCacheConnector)
    }
    "saveCorrespondenceAddress" must {
      "save correspondence address in Keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveCorrespondenceAddress(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        val result = TestCorrespondenceAddressService.saveCorrespondenceAddress(testAddress)
        await(result).get.toString must be(testAddress.toString)
        verify(mockDataCacheConnector, times(1)).saveCorrespondenceAddress(Matchers.any())(Matchers.any())
      }
    }
    "fetchCorrespondenceAddress" must {
      "return correspondence address, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchCorrespondenceAddress).thenReturn(Future.successful(Some(testAddress)))
        val result = TestCorrespondenceAddressService.fetchCorrespondenceAddress
        await(result) must be(Some(testAddress))
        verify(mockDataCacheConnector, times(1)).fetchCorrespondenceAddress(Matchers.any())
      }
      "return None, if not found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchCorrespondenceAddress).thenReturn(Future.successful(None))
        val result = TestCorrespondenceAddressService.fetchCorrespondenceAddress
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchCorrespondenceAddress(Matchers.any())
      }
    }
  }

}
