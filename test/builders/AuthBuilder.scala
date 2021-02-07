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

package builders

import models.AtedSubscriptionAuthData
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

object AuthBuilder {

  type RetrievalType = Option[CredentialRole] ~
    Option[AffinityGroup] ~
    Enrolments ~
    Option[String] ~
    Option[Credentials] ~
    Option[String]

  def buildRetrieval(atedSubscriptionAuthData: AtedSubscriptionAuthData): RetrievalType = {
    new ~(
      new ~(
        new ~(
          new ~(
            new ~(
              atedSubscriptionAuthData.credentialRole,
              atedSubscriptionAuthData.affinityGroup
            ),
            atedSubscriptionAuthData.enrolments
          ),
          atedSubscriptionAuthData.agentCode
        ),
        Some(Credentials("mockProvi", "type"))
      ),
      atedSubscriptionAuthData.groupIdentifier
    )
  }

  def produceAgentEnrolment(agentRef: String): Enrolment = {
    Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", agentRef)), "Activated")
  }

  def createUserAuthContext(userId: String, userName: String): AtedSubscriptionAuthData = {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
      None,
      Some(AffinityGroup.Organisation),
      None,
      Some("credId"),
      Some("hashed"),
      Some("testGroupId-"),
      Enrolments(Set())
    )

    atedSubscriptionAuthData
  }

  def createUserAuthContextWithoutOrg(userId: String, userName: String): AtedSubscriptionAuthData = {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
      None,
      None,
      None,
      None,
      None,
      None,
      Enrolments(Set())
    )

    atedSubscriptionAuthData
  }

  def createAgentAuthContext(userId: String, userName: String): AtedSubscriptionAuthData = {
    createAgentAuthority(agentRefNo = Some("JARN1234567"))
  }

  def createAgentUserAuthContext(userId: String, userName: String): AtedSubscriptionAuthData = {
    createAgentAuthority(agentRole = User, agentRefNo = Some("JARN1234567"))
  }

  def createNotRegisteredAgentAuthContext(userId: String, userName: String): AtedSubscriptionAuthData = {
    createAgentAuthority(agentRefNo = None)
  }

  def createAgentAssistantAuthContext(userId: String, userName: String, agentRefNo: Option[String] = None): AtedSubscriptionAuthData = {
    createAgentAuthority(Assistant, agentRefNo)
  }

  def mockAuthorisedUser(userId: String, mockAuthConnector: AuthConnector, secondExtraEnrolments: Set[Enrolment] = Set.empty) {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = AtedSubscriptionAuthData(
      None,
      Some(AffinityGroup.Organisation),
      None,
      Some("credId"),
      Some("hashed"),
      Some("testGroupId-"),
      Enrolments(Set())
    )

    val secondData = atedSubscriptionAuthData.copy(enrolments = Enrolments(
      atedSubscriptionAuthData.enrolments.enrolments ++ secondExtraEnrolments
    ))

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)), Future.successful(buildRetrieval(secondData)))
  }

  def mockAuthorisedAgent(userId: String, mockAuthConnector: AuthConnector) {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = createAgentAuthority(agentRefNo = Some("JARN1234567"))

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))
  }

  def mockAuthorisedOrgAssistant(userId: String, mockAuthConnector: AuthConnector) {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = createOrganisationAuthority(
      credRole = Assistant
    )

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))
  }

  def mockAuthorisedAgentAssistant(userId: String, mockAuthConnector: AuthConnector) {
    val atedSubscriptionAuthData: AtedSubscriptionAuthData = createAgentAuthority(
      agentRole = Assistant,
      agentRefNo = Some("JARN1234567")
    )

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(atedSubscriptionAuthData)))
  }

  def mockUnAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InvalidBearerToken("message")))
  }

  def mockUnAuthorisedUserNotLogged(mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(MissingBearerToken("message")))
  }

  private def createAgentAuthority(agentRole: CredentialRole = User, agentRefNo: Option[String]): AtedSubscriptionAuthData = {
    val agentCode = "AGENT-123"

    val agentEnrolment: Option[Enrolment] = agentRefNo.map { agentRef =>
      produceAgentEnrolment(agentRef)
    }

    val authData = AtedSubscriptionAuthData(
      Some(agentRole),
      Some(AffinityGroup.Agent),
      Some(agentCode),
      Some("credId"),
      Some("cred"),
      Some("testGroupId-"),
      Enrolments(Set(agentEnrolment).flatten)
    )

    authData
  }

  private def createOrganisationAuthority(credRole: CredentialRole): AtedSubscriptionAuthData = {

    val authData = AtedSubscriptionAuthData(
      Some(credRole),
      Some(AffinityGroup.Organisation),
      Some("credId"),
      Some("cred"),
      None,
      Some("testGroupId-"),
      Enrolments(Set())
    )

    authData
  }

}
