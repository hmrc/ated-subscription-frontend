/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class AgentClientMandateFrontendConnector @Inject()(appConfig: ApplicationConfig,
                                                    http: HttpClientV2
                                                   ) extends HeaderCarrierForPartialsConverter {

  val serviceUrl: String = appConfig.serviceUrlACMFrontend
  val emailUri = "mandate/agent/email-session"
  val displayNameUri = "mandate/agent/client-display-name-session"
  val mandateDetails = "mandate/agent/old-nonuk-mandate-from-session"
  val service = "ATED"

  def getAgentEmail(implicit request: Request[_], ec: ExecutionContext): Future[Option[AgentEmail]] = {
    val getUrl = s"$serviceUrl/$emailUri/"
    http.get(url"$getUrl").execute[Option[AgentEmail]]
  }
  def getClientDisplayName(implicit request: Request[_], ec: ExecutionContext): Future[Option[ClientDisplayName]] = {
    val getUrl = s"$serviceUrl/$displayNameUri/"
    http.get(url"$getUrl").execute[Option[ClientDisplayName]]
  }
  def getOldMandateDetails(implicit request: Request[_], ec: ExecutionContext): Future[Option[OldMandateReference]] = {
    val getUrl = s"$serviceUrl/$mandateDetails/"
    http.get(url"$getUrl").execute[HttpResponse].map(_.json.asOpt[OldMandateReference])
  }

}
