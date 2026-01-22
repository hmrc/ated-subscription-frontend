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

import javax.inject.Inject
import models._
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}


class AtedSubscriptionDataCacheConnector @Inject()(sessionCache: SessionCacheRepository) {

  import sessionCache._

  val bcSourceId: String = "BC_Business_Details"
  val bcRegDetailseId: String = "BC_BusinessReg_Details"
  val addressFormId: String = "Correspondence_Address"
  val contactFormId: String = "Contact_Details"
  val previousSubmittedFormId: String = "Previously_Submitted"
  val mandateAgentEmailFormId: String = "agent-email"
  val clientDisplayNameFormId = "client-display-name-form-id"
  val contactEmailFormId: String = "Contact_Email_Details"

  def fetchAndGetRegisteredBusinessDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessAddress]] =
    getFromSession[BusinessAddress](DataKey(bcRegDetailseId))

  def saveRegisteredBusinessDetails(businessAddress: BusinessAddress)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessAddress]] =
    putSession[BusinessAddress](DataKey(bcRegDetailseId), businessAddress).map(Some(_))

  def fetchAndGetReviewDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessCustomerDetails]] =
    getFromSession[BusinessCustomerDetails](DataKey(bcSourceId))

  def saveReviewDetails(reviewDetails: BusinessCustomerDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessCustomerDetails]] =
    putSession[BusinessCustomerDetails](DataKey(bcSourceId), reviewDetails).map(Some(_))

  def saveCorrespondenceAddress(address: Address)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Address]] =
    putSession[Address](DataKey(addressFormId), address).map(Some(_))

  def fetchCorrespondenceAddress(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Address]] =
    getFromSession[Address](DataKey(addressFormId))

  def saveContactDetails(contactDetails: ContactDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetails]] =
    putSession[ContactDetails](DataKey(contactFormId), contactDetails).map(Some(_))

  def fetchContactDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetails]] =
    getFromSession[ContactDetails](DataKey(contactFormId))

  def fetchContactDetailsEmailForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetailsEmail]] =
    getFromSession[ContactDetailsEmail](DataKey(contactEmailFormId))

  def clearCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    deleteFromSession

  def saveContactDetailsEmail(contactDetailsEmail: ContactDetailsEmail)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ContactDetailsEmail]] =
    putSession[ContactDetailsEmail](DataKey(contactEmailFormId), contactDetailsEmail).map(Some(_))

  def savePreviouslySubmitted(previousSubmittedForm: PreviousSubmittedForm)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreviousSubmittedForm]] =
    putSession[PreviousSubmittedForm](DataKey(previousSubmittedFormId), previousSubmittedForm).map(Some(_))

  def fetchPreviouslySubmittedForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreviousSubmittedForm]] =
    getFromSession[PreviousSubmittedForm](DataKey(previousSubmittedFormId))

}
