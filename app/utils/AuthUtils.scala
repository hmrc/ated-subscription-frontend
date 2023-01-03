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

package utils

import models.AtedSubscriptionAuthData
import play.api.Logging
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant, User}

object AuthUtils extends Logging {

  def isAgent(implicit user: AtedSubscriptionAuthData): Boolean =
    user.enrolments.getEnrolment("HMRC-AGENT-AGENT").isDefined || user.affinityGroup.contains(AffinityGroup.Agent)

  def isAssistant(implicit user: AtedSubscriptionAuthData): Boolean = user.credentialRole.contains(Assistant)

  def isAgentUser(implicit user: AtedSubscriptionAuthData): Boolean =
    isAgent && user.credentialRole.contains(User)

  def agentLink(implicit user: AtedSubscriptionAuthData): String = {
    user.agentCode.map(str => s"/agent/$str").getOrElse {
      logger.warn(s"[AuthUtils][getAgentLink] Exception - User does not have the correct authorisation ")
      throw new RuntimeException("User is not agent")
    }
  }

  def getAuthLink(implicit user: AtedSubscriptionAuthData): String = {
    if (isAgent) {
      agentLink
    } else {
      user.hashedCredId.map(str => s"/org/$str")
        .getOrElse(throw new RuntimeException("User is not org"))
    }
  }

  def getArn(implicit user: AtedSubscriptionAuthData): String = {
    user.enrolments.getEnrolment("HMRC-AGENT-AGENT").flatMap(_.getIdentifier("AgentRefNumber").map(_.value))
      .getOrElse(throw new RuntimeException("[getArn] No ARN found"))
  }

}
