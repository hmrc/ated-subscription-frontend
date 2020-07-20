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

package connectors

import config.AtedSessionCache
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DataCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAtedSessionCache: AtedSessionCache = mock[AtedSessionCache]
  val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true),"aa@aa.com")
  val testAgentEmail = AgentEmail("aa@aa.com")
  val clientDisplayName = ClientDisplayName("client display name")
  val testAddressForm = BusinessAddress(Some(true))
  val previouslySubmitted = PreviousSubmittedForm(Some(true))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testAtedSubscriptionDataCacheConnector = new AtedSubscriptionDataCacheConnector(mockAtedSessionCache)

  override def beforeEach(): Unit = {
    reset(mockAtedSessionCache)
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "fetch saved BusinessDetails from SessionCache" in {
        val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
          businessType = "Corporate Body",
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
        when(mockAtedSessionCache.fetchAndGetEntry[BusinessCustomerDetails](ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = testAtedSubscriptionDataCacheConnector.fetchAndGetReviewDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }
    "fetchAndGetRegisteredBusinessDetailsForSession" must {

      "fetch saved BusinessDetails address form from SessionCache" in {
        when(mockAtedSessionCache.fetchAndGetEntry[BusinessAddress](ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddressForm)))
        val result = testAtedSubscriptionDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession
        await(result).get must be (testAddressForm)
      }
    }

    "saveRegisteredBusinessDetails" must {
      "save BusinessDetails address form from SessionCache" in {
        val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_BusinessReg_Details" -> Json.toJson(testAddressForm)))
        when(mockAtedSessionCache.cache[BusinessAddress](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.saveRegisteredBusinessDetails(testAddressForm)
        await(result).get must be (testAddressForm)
      }
    }
    
    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
          businessType = "Corporate Body",
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))
        when(mockAtedSessionCache.cache[BusinessCustomerDetails](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be (reviewDetails)
      }

    }

    "saveCorrespondenceAddress" must {
      "save the correspondence address in keystore" in {
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Correspondence_Address" -> Json.toJson(testAddress)))
        when(mockAtedSessionCache.cache[Address](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.saveCorrespondenceAddress(testAddress)
        await(result).get must be (testAddress)
      }
    }

    "fetchCorrespondenceAddress" must {
      "fetch the saved correspondence address in keystore" in {
        when(mockAtedSessionCache.fetchAndGetEntry[Address](ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testAddress)))
        val result = testAtedSubscriptionDataCacheConnector.fetchCorrespondenceAddress
        await(result).get must be (testAddress)
      }
    }

    "saveContactDetails" must {
      "save the contact details in keystore" in {
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Contact_Details" -> Json.toJson(testContact)))
        when(mockAtedSessionCache.cache[ContactDetails](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.saveContactDetails(testContact)
        await(result).get must be (testContact)
      }
    }
    "saveContactDetailsEmail" must {
      "save the contact details email in keystore" in {
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Contact_Email_Details" -> Json.toJson(testContactEmail)))
        when(mockAtedSessionCache.cache[ContactDetailsEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.saveContactDetailsEmail(testContactEmail)
        await(result).get must be (testContactEmail)
      }
    }

    "fetchContactDetailsForSession" must {
      "fetch the saved contact details in keystore" in {
        when(mockAtedSessionCache.fetchAndGetEntry[ContactDetails](ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContact)))
        val result = testAtedSubscriptionDataCacheConnector.fetchContactDetailsForSession
        await(result).get must be (testContact)
      }
    }
    "fetchContactDetailsEmailForSession" must {
      "fetch the saved contact details in keystore" in {
        when(mockAtedSessionCache.fetchAndGetEntry[ContactDetailsEmail](ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testContactEmail)))
        val result = testAtedSubscriptionDataCacheConnector.fetchContactDetailsEmailForSession
        await(result).get must be (testContactEmail)
      }
    }

    "savePreviouslySubmitted" must {
      "save the previously submitted ATED returns question in keystore" in {
        val returnedCacheMap: CacheMap = CacheMap("data", Map("Previously_Submitted" -> Json.toJson(previouslySubmitted)))
        when(mockAtedSessionCache.cache[PreviousSubmittedForm](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))
        val result = testAtedSubscriptionDataCacheConnector.savePreviouslySubmitted(previouslySubmitted)
        await(result).get must be (previouslySubmitted)
      }
    }

    "fetchPreviouslySubmitted" must {
      "fetch the previously submitted ATED returns question in keystore" in {
        when(mockAtedSessionCache.fetchAndGetEntry[PreviousSubmittedForm](ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(previouslySubmitted)))
        val result = testAtedSubscriptionDataCacheConnector.fetchPreviouslySubmittedForSession
        await(result).get must be (previouslySubmitted)
      }
    }

    "clearCache" must {
      "clear the local keystore" in {
        when(mockAtedSessionCache.remove()(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        await(testAtedSubscriptionDataCacheConnector.clearCache).status must be(OK)
      }
    }
  }
}
