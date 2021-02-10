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

package connectors

import config.AtedSessionCache
import javax.inject.Inject
import models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}


class AtedSubscriptionDataCacheConnector @Inject()(sessionCache: AtedSessionCache) {
  val bcSourceId: String = "BC_Business_Details"
  val bcRegDetailseId: String = "BC_BusinessReg_Details"
  val addressFormId: String = "Correspondence_Address"
  val contactFormId: String = "Contact_Details"
  val previousSubmittedFormId: String = "Previously_Submitted"
  val mandateAgentEmailFormId: String = "agent-email"
  val clientDisplayNameFormId = "client-display-name-form-id"
  val contactEmailFormId: String = "Contact_Email_Details"

  def fetchAndGetRegisteredBusinessDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessAddress]] = {
    sessionCache.fetchAndGetEntry[BusinessAddress](bcRegDetailseId)
  }

  def saveRegisteredBusinessDetails(businessAddress: BusinessAddress)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessAddress]] = {
     sessionCache.cache[BusinessAddress](bcRegDetailseId, businessAddress) map {
         data => data.getEntry[BusinessAddress](bcRegDetailseId)
       }
  }

  def fetchAndGetReviewDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessCustomerDetails]] = {
    sessionCache.fetchAndGetEntry[BusinessCustomerDetails](bcSourceId)
  }

  def saveReviewDetails(reviewDetails: BusinessCustomerDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessCustomerDetails]] = {
    sessionCache.cache[BusinessCustomerDetails](bcSourceId, reviewDetails) map  {
      data => data.getEntry[BusinessCustomerDetails](bcSourceId) }
  }

  def saveCorrespondenceAddress(address: Address)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Address]] = {
    sessionCache.cache[Address](addressFormId, address) map { cachedData =>
      cachedData.getEntry[Address](addressFormId)
    }
  }

  def fetchCorrespondenceAddress(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Address]] = {
    sessionCache.fetchAndGetEntry[Address](addressFormId)
  }

  def saveContactDetails(contactDetails: ContactDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetails]] = {
    sessionCache.cache[ContactDetails](contactFormId, contactDetails) map { cachedData =>
      cachedData.getEntry[ContactDetails](contactFormId)
    }
  }

  def fetchContactDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetails]] = {
    sessionCache.fetchAndGetEntry[ContactDetails](contactFormId)
  }

  def fetchContactDetailsEmailForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetailsEmail]] = {
    sessionCache.fetchAndGetEntry[ContactDetailsEmail](contactEmailFormId)
  }

  def clearCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    sessionCache.remove()
  }

  def saveContactDetailsEmail(contactDetailsEmail: ContactDetailsEmail)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetailsEmail]] = {
    sessionCache.cache[ContactDetailsEmail](contactEmailFormId, contactDetailsEmail) map { cachedData =>
      cachedData.getEntry[ContactDetailsEmail](contactEmailFormId)
    }
  }

  def savePreviouslySubmitted(previousSubmittedForm: PreviousSubmittedForm)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreviousSubmittedForm]] = {
    sessionCache.cache[PreviousSubmittedForm](previousSubmittedFormId, previousSubmittedForm) map { cachedData =>
      cachedData.getEntry[PreviousSubmittedForm](previousSubmittedFormId)
    }
  }

  def fetchPreviouslySubmittedForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreviousSubmittedForm]] = {
    sessionCache.fetchAndGetEntry[PreviousSubmittedForm](previousSubmittedFormId)
  }

}


