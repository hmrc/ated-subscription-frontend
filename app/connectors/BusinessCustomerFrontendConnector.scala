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

import config.WSHttp
import models.ReviewDetails
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HttpGet, HttpResponse }

trait BusinessCustomerFrontendConnector extends ServicesConfig  with RawResponseReads with HeaderCarrierForPartialsConverter {

  def serviceUrl = baseUrl("business-customer-frontend")
  val businessCustomerUri = "business-customer"
  val reviewDetailsUri = "fetch-review-details"
  val service = "ATED"
  val http: HttpGet

  def getReviewDetails(implicit request: Request[_], ac: AuthContext): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/$businessCustomerUri/$reviewDetailsUri/$service"
    http.GET(getUrl)
  }
}

object BusinessCustomerFrontendConnector extends BusinessCustomerFrontendConnector {
  // $COVERAGE-OFF$
  val http = WSHttp
  override def crypto: (String) => String = SessionCookieCryptoFilter.encrypt _
  // $COVERAGE-ON$
}
