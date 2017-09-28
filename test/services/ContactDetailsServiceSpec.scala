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

import connectors.DataCacheConnector
import models.{ContactDetails, ContactDetailsEmail}
import org.mockito.Matchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ContactDetailsServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")

  object TestContactDetailsService extends ContactDetailsService {
    val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockDataCacheConnector)
  }

  "ContactDetailsService" must {
    "saveContactDetails" must {
      "save Contact details into keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveContactDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        val result = TestContactDetailsService.saveContactDetails(testContact)
        await(result).get.toString must be(testContact.toString)
        verify(mockDataCacheConnector, times(1)).saveContactDetails(Matchers.any())(Matchers.any())
      }
    }
    "saveContactDetailsEmail" must {
      "save Contact details email into keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveContactDetailsEmail(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        val result = TestContactDetailsService.saveContactDetailsEmail(testContactEmail)
        await(result).get.toString must be(testContactEmail.toString)
        verify(mockDataCacheConnector, times(1)).saveContactDetailsEmail(Matchers.any())(Matchers.any())
      }
    }

    "fetchContactDetails" must {
      "return contact details, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsForSession).thenReturn(Future.successful(Some(testContact)))
        val result = TestContactDetailsService.fetchContactDetails
        await(result) must be(Some(testContact))
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsForSession(Matchers.any())
      }
      "return None, if not found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsForSession).thenReturn(Future.successful(None))
        val result = TestContactDetailsService.fetchContactDetails
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsForSession(Matchers.any())
      }

      "return contact details email, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession).thenReturn(Future.successful(Some(testContactEmail)))
        val result = TestContactDetailsService.fetchContactDetailsEmail
        await(result) must be(Some(testContactEmail))
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsEmailForSession(Matchers.any())
      }
      "return None, if not found email in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession).thenReturn(Future.successful(None))
        val result = TestContactDetailsService.fetchContactDetailsEmail
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsEmailForSession(Matchers.any())
      }
    }
  }

}
