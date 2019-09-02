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

package controllers.auth

import models.AtedSubscriptionAuthData
import org.apache.commons.codec.binary.Base64.encodeBase64String
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthFunctionality extends AuthorisedFunctions {

  val agentEnrolment = "HMRC-AGENT-AGENT"
  val atedEnrolment = "HMRC-ATED-ORG"

  lazy val signInUrl: String = ExternalUrls.loginURL
  val origin: String = "ated-subscription-frontend"

  def loginParams: Map[String, Seq[String]] = Map(
    "continue" -> Seq(signInUrl),
    "origin" -> Seq(origin)
  )

  def authoriseFor[A](body: AtedSubscriptionAuthData => Future[Result])
                     (implicit hc: HeaderCarrier, ec: ExecutionContext, req: Request[_], messages: Messages): Future[Result] = {
    authorised(
      (Enrolment(agentEnrolment) or
        Enrolment(atedEnrolment) or
        AffinityGroup.Organisation or
        AffinityGroup.Agent)
      and AuthProviders(GovernmentGateway)
    )
      .retrieve(
        Retrievals.credentialRole and
          Retrievals.affinityGroup and
          Retrievals.allEnrolments and
          Retrievals.agentCode and
          Retrievals.credentials
      ) {
        case credRole ~ affinityGroup ~ authorisedEnrolments ~ agentCode ~ credentials =>
          body(AtedSubscriptionAuthData(
            credRole,
            affinityGroup,
            agentCode,
            credentials.map(creds => UrlSafe.hash(creds.providerId)),
            authorisedEnrolments
          ))
      } recover {
        case _: MissingBearerToken =>
          Redirect(ExternalUrls.loginURL, loginParams)
        case er: AuthorisationException =>
          Logger.error(s"[recoverAuthorisedCalls] Auth exception: $er")
          Redirect(controllers.routes.ApplicationController.unauthorised().url)
      }
  }
}

object UrlSafe {

  def hash(value: String): String = {
    val sha1: Array[Byte] = DigestUtils.sha1(value)
    val encoded = encodeBase64String(sha1)

    urlSafe(encoded)
  }

  private def urlSafe(encoded: String): String = {
    encoded.replace("=", "")
      .replace("/", "_")
      .replace("+", "-")
  }
}
