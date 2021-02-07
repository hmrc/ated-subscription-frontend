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

package utils

import builders.AuthBuilder
import org.scalatestplus.play.PlaySpec

class AuthUtilsSpec extends PlaySpec {

  "isAgent" must {
    "Return true if this user is an agent" in {
      implicit val auth = builders.AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAgent must be(true)
    }

    "Return false if this user is not an agent" in {
      implicit val auth = builders.AuthBuilder.createUserAuthContext("userId", "Joe Bloggs")
      AuthUtils.isAgent must be(false)
    }
  }

  "isAssistant" must {
    "return true if user is an Assistant" in {
      implicit val auth = builders.AuthBuilder.createAgentAssistantAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAssistant must be(true)
    }

    "return false if user is not an Assistant" in {
      implicit val auth = builders.AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAssistant must be(false)
    }
  }

  "isAgentUser" must {
    "return true if user is Admin Agent" in {
      implicit val auth = builders.AuthBuilder.createAgentAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAgentUser must be(true)
    }

    "return true if user is User Agent" in {
      implicit val auth = builders.AuthBuilder.createAgentUserAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAgentUser must be(true)
    }

    "return false if user is Not Admin Agent" in {
      implicit val auth = builders.AuthBuilder.createAgentAssistantAuthContext("agentId", "Agent Bloggs")
      AuthUtils.isAgentUser must be(false)
    }
  }

  "agentLink" must {
    "throw Runtime exception is thrown, if user does not have agent account" in {
      implicit val user = AuthBuilder.createUserAuthContextWithoutOrg("userId", "joe bloggs")
      val thrown = the[RuntimeException] thrownBy AuthUtils.agentLink
      thrown.getMessage must include("User is not agent")
    }
  }
  "getAuthLink" must {
    "Runtime exception is thrown, if link is not found in org account" in {
      implicit val user = AuthBuilder.createUserAuthContextWithoutOrg("userId", "joe bloggs")
      val thrown = the[RuntimeException] thrownBy AuthUtils.getAuthLink
      thrown.getMessage must include("User is not org")
    }
    "return agent link, if user has agent account" in {
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      AuthUtils.getAuthLink must be("/agent/AGENT-123")
    }
    "return org link, if user has org account" in {
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
      AuthUtils.getAuthLink must be("/org/hashed")
    }
  }

  "getArn" must {
    "return ARN, if agent is registered and has ARN in authContext" in {
      implicit val user = AuthBuilder.createAgentAuthContext("userId", "joe bloggs")
      AuthUtils.getArn must be("JARN1234567")
    }
    "throw exception, if agent is not registered and doesn't have ARN in authContext" in {
      implicit val user = AuthBuilder.createNotRegisteredAgentAuthContext("userId", "joe bloggs")
      val thrown = the[RuntimeException] thrownBy AuthUtils.getArn
      thrown.getMessage must be("[getArn] No ARN found")
    }
  }



}
