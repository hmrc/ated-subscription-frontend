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

package utils

import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{AgentAdmin, AgentAssistant}

object AuthUtils {

  def isAgent(implicit user: AuthContext): Boolean = user.principal.accounts.agent.isDefined

  def isAgentAssistant(implicit user: AuthContext): Boolean = user.principal.accounts.agent.map(_.agentUserRole).contains(AgentAssistant)

  def isAgentAdmin(implicit user: AuthContext): Boolean = user.principal.accounts.agent.map(_.agentUserRole).contains(AgentAdmin)

  def agentLink(implicit user: AuthContext): String = {
    user.principal.accounts.agent.map(_.link).getOrElse {
      Logger.warn(s"[AuthUtils][getAgentLink] Exception - User does not have the correct authorisation ")
      throw new RuntimeException("User is not agent")
    }
  }

  def getAuthLink(implicit user: AuthContext): String = {
    if (isAgent) agentLink
    else user.principal.accounts.org.map(_.link).getOrElse(throw new RuntimeException("User is not org"))
  }

  def getArn(implicit user: AuthContext): String = {
    user.principal.accounts.agent.flatMap(_.agentBusinessUtr).map(_.utr).getOrElse(throw new RuntimeException("ARN not found"))
  }

}
