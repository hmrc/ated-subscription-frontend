/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{ContactDetails, ContactDetailsEmail}
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

class ContactDetailsServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockDataCacheConnector: AtedSubscriptionDataCacheConnector = mock[AtedSubscriptionDataCacheConnector]
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true), "abc@test.com")

  val testContactDetailsService: ContactDetailsService = new ContactDetailsService(mockDataCacheConnector)

  override def beforeEach: Unit = {
    reset(mockDataCacheConnector)
  }

  "ContactDetailsService" must {
    "saveContactDetails" must {
      "save Contact details into keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))
        val result = testContactDetailsService.saveContactDetails(testContact)
        await(result).get.toString must be(testContact.toString)
        verify(mockDataCacheConnector, times(1)).saveContactDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
    "saveContactDetailsEmail" must {
      "save Contact details email into keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.saveContactDetailsEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        val result = testContactDetailsService.saveContactDetailsEmail(testContactEmail)
        await(result).get.toString must be(testContactEmail.toString)
        verify(mockDataCacheConnector, times(1)).saveContactDetailsEmail(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }

    "fetchContactDetails" must {
      "return contact details, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsForSession).thenReturn(Future.successful(Some(testContact)))
        val result = testContactDetailsService.fetchContactDetails
        await(result) must be(Some(testContact))
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
      "return None, if not found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsForSession).thenReturn(Future.successful(None))
        val result = testContactDetailsService.fetchContactDetails
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "return contact details email, if found in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession).thenReturn(Future.successful(Some(testContactEmail)))
        val result = testContactDetailsService.fetchContactDetailsEmail
        await(result) must be(Some(testContactEmail))
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsEmailForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
      "return None, if not found email in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockDataCacheConnector.fetchContactDetailsEmailForSession).thenReturn(Future.successful(None))
        val result = testContactDetailsService.fetchContactDetailsEmail
        await(result) must be(None)
        verify(mockDataCacheConnector, times(1)).fetchContactDetailsEmailForSession(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }

}
