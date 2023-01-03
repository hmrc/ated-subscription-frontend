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

import org.scalatestplus.play.PlaySpec
import utils.BusinessTypeConstants._

class BusinessTypesConstantsSpec extends PlaySpec {

  ".saBusinessTypes" when {
    "asked for a list of all sa business types" should {
      "return a valid list" in {
        BusinessTypeConstants.saBusinessTypes must be(
          List(businessPartnership, limitedPartnership, limitedLiabilityPartnership, overseasCompany, soleTrader)
        )
      }
    }

    "asked for a list of all business types" should {
      "return a valid list" in {
        BusinessTypeConstants.allBusinessTypes must be(
          List(businessPartnership, limitedPartnership, limitedLiabilityPartnership,
            overseasCompany, soleTrader, limitedCompany, unitTrust, unlimitedCompany)
        )
      }
    }
  }
}
