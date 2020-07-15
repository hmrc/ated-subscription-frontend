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

import config.ApplicationConfig
import javax.inject.Inject
import models.{AgentEmail, ClientDisplayName, OldMandateReference}
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.{ExecutionContext, Future}

class AgentClientMandateFrontendConnector @Inject()(appConfig: ApplicationConfig,
                                                    http: DefaultHttpClient
                                                   ) extends RawResponseReads with HeaderCarrierForPartialsConverter {

  val serviceUrl: String = appConfig.serviceUrlACMFrontend
  val emailUri = "mandate/agent/email-session"
  val displayNameUri = "mandate/agent/client-display-name-session"
  val mandateDetails = "mandate/agent/old-nonuk-mandate-from-session"
  val service = "ATED"

  override def crypto: String => String = identity

  def getAgentEmail(implicit request: Request[_], ec: ExecutionContext): Future[Option[AgentEmail]] = {
    val getUrl = s"$serviceUrl/$emailUri/"
    http.GET[Option[AgentEmail]](getUrl)
  }

  def getClientDisplayName(implicit request: Request[_], ec: ExecutionContext): Future[Option[ClientDisplayName]] = {
    val getUrl = s"$serviceUrl/$displayNameUri/"
    http.GET[Option[ClientDisplayName]](getUrl)
  }

  def getOldMandateDetails(implicit request: Request[_], ec: ExecutionContext): Future[Option[OldMandateReference]] = {
    val getUrl = s"$serviceUrl/$mandateDetails/"
    http.GET(getUrl) map { response => response.json.asOpt[OldMandateReference]}
  }

}