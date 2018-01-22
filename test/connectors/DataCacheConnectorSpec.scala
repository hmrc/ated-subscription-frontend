/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import config.AtedSessionCache
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockBCSessionCache = mock[SessionCache]
  val mockSessionCache = mock[SessionCache]
  val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true),"aa@aa.com")
  val testAgentEmail = AgentEmail("aa@aa.com")
  val clientDisplayName = ClientDisplayName("client display name")
  val testAddressForm = BusinessAddress(Some(true))


  object TestBCDataCacheConnector extends DataCacheConnector {
    val sessionCache: SessionCache = mockBCSessionCache
  }

  object TestDataCacheConnector extends DataCacheConnector {
    val sessionCache: SessionCache = mockSessionCache
  }

  override def beforeEach(): Unit = {
    reset(mockBCSessionCache)
    reset(mockSessionCache)
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache for Ated" in {
        AtedSubscriptionDataCacheConnector.sessionCache must be(AtedSessionCache)
      }

      "fetch saved BusinessDetails from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails = ReviewDetails(businessName = "ACME",
          businessType = Some("corporate body"),
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
        when(mockBCSessionCache.fetchAndGetEntry[ReviewDetails](Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = TestBCDataCacheConnector.fetchAndGetReviewDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }
    "fetchAndGetRegisteredBusinessDetailsForSession" must {

      "use the correct session cache for Ated" in {
        AtedSubscriptionDataCacheConnector.sessionCache must be(AtedSessionCache)
      }

      "fetch saved BusinessDetails address form from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[BusinessAddress](Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAddressForm)))
        val result = TestDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession
        await(result).get must be (testAddressForm)
      }
    }

    "saveRegisteredBusinessDetails" must {
      "save BusinessDetails address form from SessionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_BusinessReg_Details" -> Json.toJson(testAddressForm)))
        when(mockSessionCache.cache[BusinessAddress](Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveRegisteredBusinessDetails(testAddressForm)
        await(result).get must be (testAddressForm)
      }
    }
    
    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val reviewDetails = ReviewDetails(businessName = "ACME",
          businessType = Some("corporate body"),
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))
        when(mockBCSessionCache.cache[ReviewDetails](Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestBCDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be (reviewDetails)
      }

    }

    "saveCorrespondenceAddress" must {
      "save the correspondence address in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Correspondence_Address" -> Json.toJson(testAddress)))
        when(mockSessionCache.cache[Address](Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveCorrespondenceAddress(testAddress)
        await(result).get must be (testAddress)
      }
    }

    "fetchCorrespondenceAddress" must {
      "fetch the saved correspondence address in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[Address](Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testAddress)))
        val result = TestDataCacheConnector.fetchCorrespondenceAddress
        await(result).get must be (testAddress)
      }
    }

    "saveContactDetails" must {
      "save the contact details in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Contact_Details" -> Json.toJson(testContact)))
        when(mockSessionCache.cache[ContactDetails](Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveContactDetails(testContact)
        await(result).get must be (testContact)
      }
    }
    "saveContactDetailsEmail" must {
      "save the contact details email in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Contact_Email_Details" -> Json.toJson(testContactEmail)))
        when(mockSessionCache.cache[ContactDetailsEmail](Matchers.any(), Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = TestDataCacheConnector.saveContactDetailsEmail(testContactEmail)
        await(result).get must be (testContactEmail)
      }
    }

    "fetchContactDetailsForSession" must {
      "fetch the saved contact details in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[ContactDetails](Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testContact)))
        val result = TestDataCacheConnector.fetchContactDetailsForSession
        await(result).get must be (testContact)
      }
    }
    "fetchContactDetailsEmailForSession" must {
      "fetch the saved contact details in keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.fetchAndGetEntry[ContactDetailsEmail](Matchers.any())(Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        val result = TestDataCacheConnector.fetchContactDetailsEmailForSession
        await(result).get must be (testContactEmail)
      }
    }

    "clearCache" must {
      "clear the local keystore" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSessionCache.remove()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        await(TestDataCacheConnector.clearCache).status must be(OK)
      }
    }

  }

}
