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

package connectors

import config.AtedSessionCache
import models._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataCacheConnector {

  val sessionCache: SessionCache

  val bcSourceId: String = "BC_Business_Details"
  val bcRegDetailseId: String = "BC_BusinessReg_Details"
  val addressFormId: String = "Correspondence_Address"
  val contactFormId: String = "Contact_Details"
  val mandateAgentEmailFormId: String = "agent-email"
  val clientDisplayNameFormId = "client-display-name-form-id"
  val contactEmailFormId: String = "Contact_Email_Details"



  def fetchAndGetRegisteredBusinessDetailsForSession(implicit hc: HeaderCarrier): Future[Option[BusinessAddress]] = {
    sessionCache.fetchAndGetEntry[BusinessAddress](bcRegDetailseId)
  }

  def saveRegisteredBusinessDetails(businessAddress: BusinessAddress)(implicit hc: HeaderCarrier): Future[Option[BusinessAddress]] = {
     sessionCache.cache[BusinessAddress](bcRegDetailseId, businessAddress)flatMap
       { data => Future.successful(data.getEntry[BusinessAddress](bcRegDetailseId)) }
  }




  def fetchAndGetReviewDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    sessionCache.fetchAndGetEntry[ReviewDetails](bcSourceId)
  }

  def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    val result = sessionCache.cache[ReviewDetails](bcSourceId, reviewDetails)
    result flatMap { data => Future.successful(data.getEntry[ReviewDetails](bcSourceId)) }
  }

  def saveCorrespondenceAddress(address: Address)(implicit hc: HeaderCarrier): Future[Option[Address]] = {
    sessionCache.cache[Address](addressFormId, address) flatMap { cachedData =>
      Future.successful(cachedData.getEntry[Address](addressFormId))
    }
  }

  def fetchCorrespondenceAddress(implicit hc: HeaderCarrier): Future[Option[Address]] = {
    sessionCache.fetchAndGetEntry[Address](addressFormId)
  }

  def saveContactDetails(contactDetails: ContactDetails)(implicit hc: HeaderCarrier): Future[Option[ContactDetails]] = {
    sessionCache.cache[ContactDetails](contactFormId, contactDetails) flatMap { cachedData =>
      Future.successful(cachedData.getEntry[ContactDetails](contactFormId))
    }
  }

  def fetchContactDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ContactDetails]] = {
    sessionCache.fetchAndGetEntry[ContactDetails](contactFormId)
  }

  def fetchContactDetailsEmailForSession(implicit hc: HeaderCarrier): Future[Option[ContactDetailsEmail]] = {
    sessionCache.fetchAndGetEntry[ContactDetailsEmail](contactEmailFormId)
  }

  def clearCache(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    sessionCache.remove()
  }

  def saveContactDetailsEmail(contactDetailsEmail: ContactDetailsEmail)(implicit hc: HeaderCarrier): Future[Option[ContactDetailsEmail]] = {
    sessionCache.cache[ContactDetailsEmail](contactEmailFormId, contactDetailsEmail) flatMap { cachedData =>
      Future.successful(cachedData.getEntry[ContactDetailsEmail](contactEmailFormId))
    }
  }

}

object AtedSubscriptionDataCacheConnector extends DataCacheConnector {
  val sessionCache: SessionCache = AtedSessionCache
}
