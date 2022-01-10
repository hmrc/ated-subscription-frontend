/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import play.api.libs.json.{Json, OFormat}

case class EnrolRequest(portalId: String, serviceName: String, friendlyName: String,  knownFacts: Seq[String])

object EnrolRequest {
  implicit val formats: OFormat[EnrolRequest] = Json.format[EnrolRequest]
}

case class Identifier(`type`: String, value: String)

object Identifier {
  implicit val formats: OFormat[Identifier] = Json.format[Identifier]
}

case class EnrolResponse(serviceName: String, state:String, identifiers: Seq[Identifier])

object EnrolResponse {
  implicit val formats: OFormat[EnrolResponse] = Json.format[EnrolResponse]
}
