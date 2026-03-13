/*
 * Copyright 2025 HM Revenue & Customs
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

import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DataCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]
  val testAddress = Address("line_1", "line_2", None, None, None, "U.K.")
  val testContact = ContactDetails("ABC", "DEF", "1234567890")
  val testContactEmail = ContactDetailsEmail(Some(true),"aa@aa.com")
  val testAgentEmail = AgentEmail("aa@aa.com")
  val clientDisplayName = ClientDisplayName("client display name")
  val testAddressForm = BusinessAddress(Some(true))
  val previouslySubmitted = PreviousSubmittedForm(Some(true))
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  val testAtedSubscriptionDataCacheConnector = new AtedSubscriptionDataCacheConnector(mockSessionCacheRepo)

  override def beforeEach(): Unit = {
    reset(mockSessionCacheRepo)
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "fetch saved BusinessDetails from SessionCache" in {
        val reviewDetails = BusinessCustomerDetails(businessName = "ACME",
          businessType = "Corporate Body",
          businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
          sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))
        when(mockSessionCacheRepo.getFromSession[BusinessCustomerDetails](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(reviewDetails)))
        val result = testAtedSubscriptionDataCacheConnector.fetchAndGetReviewDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }
    "fetchAndGetRegisteredBusinessDetailsForSession" must {

      "fetch saved BusinessDetails address form from SessionCache" in {
        when(mockSessionCacheRepo.getFromSession[BusinessAddress](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(testAddressForm)))
        val result = testAtedSubscriptionDataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession
        await(result).get must be (testAddressForm)
      }
    }

    "saveRegisteredBusinessDetails" must {
      "save BusinessDetails address form from SessionCache" in {
        when(mockSessionCacheRepo.putSession[BusinessAddress](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(testAddressForm))(any(), any(), any())).thenReturn(Future.successful(testAddressForm))
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
        when(mockSessionCacheRepo.putSession[BusinessCustomerDetails](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(reviewDetails))(any(), any(), any())).thenReturn(Future.successful(reviewDetails))
        val result = testAtedSubscriptionDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be (reviewDetails)
      }

    }

    "saveCorrespondenceAddress" must {
      "save the correspondence address in mongo" in {
        when(mockSessionCacheRepo.putSession[Address](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(testAddress))(any(), any(), any())).thenReturn(Future.successful(testAddress))
        val result = testAtedSubscriptionDataCacheConnector.saveCorrespondenceAddress(testAddress)
        await(result).get must be (testAddress)
      }
    }

    "fetchCorrespondenceAddress" must {
      "fetch the saved correspondence address from mongo" in {
        when(mockSessionCacheRepo.getFromSession[Address](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(testAddress)))
        val result = testAtedSubscriptionDataCacheConnector.fetchCorrespondenceAddress
        await(result).get must be (testAddress)
      }
    }

    "saveContactDetails" must {
      "save the contact details in mongo" in {
        when(mockSessionCacheRepo.putSession[ContactDetails](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(testContact))(any(), any(), any())).thenReturn(Future.successful(testContact))
        val result = testAtedSubscriptionDataCacheConnector.saveContactDetails(testContact)
        await(result).get must be (testContact)
      }
    }
    "saveContactDetailsEmail" must {
      "save the contact details email in mongo" in {
        when(mockSessionCacheRepo.putSession[ContactDetailsEmail](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(testContactEmail))(any(), any(), any())).thenReturn(Future.successful(testContactEmail))
        val result = testAtedSubscriptionDataCacheConnector.saveContactDetailsEmail(testContactEmail)
        await(result).get must be (testContactEmail)
      }
    }

    "fetchContactDetailsForSession" must {
      "fetch the saved contact details from mongo" in {
        when(mockSessionCacheRepo.getFromSession[ContactDetails](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(testContact)))
        val result = testAtedSubscriptionDataCacheConnector.fetchContactDetailsForSession
        await(result).get must be (testContact)
      }
    }
    "fetchContactDetailsEmailForSession" must {
      "fetch the saved contact details from mongo" in {
        when(mockSessionCacheRepo.getFromSession[ContactDetailsEmail](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(testContactEmail)))
        val result = testAtedSubscriptionDataCacheConnector.fetchContactDetailsEmailForSession
        await(result).get must be (testContactEmail)
      }
    }

    "savePreviouslySubmitted" must {
      "save the previously submitted ATED returns question in mongo" in {
        when(mockSessionCacheRepo.putSession[PreviousSubmittedForm](
          DataKey(ArgumentMatchers.any()), ArgumentMatchers.eq(previouslySubmitted))(any(), any(), any())).thenReturn(Future.successful(previouslySubmitted))
        val result = testAtedSubscriptionDataCacheConnector.savePreviouslySubmitted(previouslySubmitted)
        await(result).get must be (previouslySubmitted)
      }
    }

    "fetchPreviouslySubmitted" must {
      "fetch the previously submitted ATED returns question from mongo" in {
        when(mockSessionCacheRepo.getFromSession[PreviousSubmittedForm](
          DataKey(ArgumentMatchers.any()))(any(), any())).thenReturn(Future.successful(Some(previouslySubmitted)))
        val result = testAtedSubscriptionDataCacheConnector.fetchPreviouslySubmittedForSession
        await(result).get must be (previouslySubmitted)
      }
    }

    "clearCache" must {
      "clear the local session cache" in {
        when(mockSessionCacheRepo.deleteFromSession(any())).thenReturn(Future.successful(()))
        await(testAtedSubscriptionDataCacheConnector.clearCache) must be(())
      }
    }
  }
}
