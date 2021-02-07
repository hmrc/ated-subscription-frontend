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

import java.util.UUID
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

object SessionBuilder {
  val sessionId = s"session-${UUID.randomUUID}"
  def updateRequestWithSession[T](fakeRequest: FakeRequest[T], userId: String): FakeRequest[T] = {
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "faketoken",
      "userId" -> userId)
  }

  def buildRequestWithSession(userId: String, email: Option[String] = None): FakeRequest[AnyContentAsEmpty.type] = {
    email match {
      case Some(emailId) => FakeRequest().withSession(
        "sessionId" -> sessionId,
        "token" -> "faketoken",
        "userId" -> userId,
        "email" -> emailId
      )
      case None => FakeRequest().withSession(
        "sessionId" -> sessionId,
        "token" -> "faketoken",
        "userId" -> userId
      )
    }
  }

  def buildRequestWithSessionNoUser() = {
    FakeRequest().withSession(
      "sessionId" -> sessionId)
  }

}