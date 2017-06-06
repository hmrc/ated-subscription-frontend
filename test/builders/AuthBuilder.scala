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

package builders

import net.sourceforge.htmlunit.corejs.javascript.NativeGenerator.GeneratorClosedException
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{ConfidenceLevel, CredentialStrength, _}

import scala.concurrent.Future

object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String): AuthContext = {
    val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
    AuthContext(authority = orgAuthority, nameFromSession = Some(userName))
  }

  def createUserAuthContextWithoutOrg(userId: String, userName: String): AuthContext = {
    val orgAuthority = Authority(userId, Accounts(org = None), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
    AuthContext(authority = orgAuthority, nameFromSession = Some(userName))
  }

  def createAgentAuthContext(userId: String, userName: String): AuthContext = {
    AuthContext(authority = createAgentAuthority(userId, agentRefNo = Some("JARN1234567")), nameFromSession = Some(userName))
  }

  def createNotRegisteredAgentAuthContext(userId: String, userName: String): AuthContext = {
    AuthContext(authority = createAgentAuthority(userId, agentRefNo = None), nameFromSession = Some(userName))
  }

  def createAgentAssistantAuthContext(userId: String, userName: String, agentRefNo: Option[String] = None): AuthContext = {
    AuthContext(authority = createAgentAuthority(userId, AgentAssistant, agentRefNo), nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
      Future.successful(Some(orgAuthority))
    }
  }

  def mockAuthorisedAgent(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createAgentAuthority(userId, agentRefNo = Some("JARN1234567"))))
    }
  }

  def mockAuthorisedAgentAssistant(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createAgentAuthority(userId, agentRole = AgentAssistant, agentRefNo = Some("JARN1234567"))))
    }
  }

  def mockUnAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    val x = new Generator()
    val nino = x.nextNino
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(s"paye/$nino", nino))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
      Future.successful(Some(payeAuthority))
    }
  }

  private def createAgentAuthority(userId: String, agentRole: AgentRole = AgentAdmin, agentRefNo: Option[String] = None): Authority = {
    val agentCode = "AGENT-123"
    val agentBusinessUtr = agentRefNo.map { agentRef =>
      AgentBusinessUtr(agentRef)
    }

    val agentAccount = AgentAccount(link = s"agent/$agentCode",
      agentCode = AgentCode(agentCode),
      agentUserId = AgentUserId(userId),
      agentUserRole = agentRole,
      payeReference = None,
      agentBusinessUtr = agentBusinessUtr)
    Authority(userId, Accounts(agent = Some(agentAccount)), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

}
