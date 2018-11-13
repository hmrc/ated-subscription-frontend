/*
 * Copyright 2018 HM Revenue & Customs
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

trait ValidationConstants {

  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r
  val addressLineLength = 35
  val postcodeLength = 9
  val countryLength = 2
  val emailLength = 132
  val lengthZero = 0
  val nameLength = 35
  val phoneLength = 24
  val telephoneRegex = "^[A-Z0-9 )/(\\-*#]+$".r
  val nameRegex = "^[a-zA-Z &`\\-\'^]{1,35}$"
  val addressLineRegex = "^[A-Za-z0-9 \\-,.&']{1,35}$"
  val postCodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,10}$"

}
