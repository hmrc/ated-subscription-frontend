/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{ContactDetails, ContactDetailsEmail}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait ContactDetailsService {

  val dataCacheConnector: DataCacheConnector

  def saveContactDetails(contactDetails: ContactDetails)(implicit hc: HeaderCarrier): Future[Option[ContactDetails]] = {
    dataCacheConnector.saveContactDetails(contactDetails)
  }

  def fetchContactDetails(implicit hc: HeaderCarrier): Future[Option[ContactDetails]] = {
    dataCacheConnector.fetchContactDetailsForSession
  }

  def fetchContactDetailsEmail(implicit hc: HeaderCarrier): Future[Option[ContactDetailsEmail]] = {
    dataCacheConnector.fetchContactDetailsEmailForSession
  }

  def saveContactDetailsEmail(contactDetailsEmail: ContactDetailsEmail)(implicit hc: HeaderCarrier): Future[Option[ContactDetailsEmail]] = {
    dataCacheConnector.saveContactDetailsEmail(contactDetailsEmail)
  }

}

object ContactDetailsService extends ContactDetailsService {
  val dataCacheConnector: DataCacheConnector = AtedSubscriptionDataCacheConnector
}
