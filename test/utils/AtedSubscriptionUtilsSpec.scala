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

package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class AtedSubscriptionUtilsSpec extends PlaySpec with OneServerPerSuite {

  "AtedSubscriptionUtils" must {

    "getSelectedCountry" must {
      "bring the correct country from the file" in {
        AtedSubscriptionUtils.getSelectedCountry("GB") must be("United Kingdom")
        AtedSubscriptionUtils.getSelectedCountry("US") must be("USA")
        AtedSubscriptionUtils.getSelectedCountry("VG") must be("British Virgin Islands")
        AtedSubscriptionUtils.getSelectedCountry("UG") must be("Uganda")
        AtedSubscriptionUtils.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        AtedSubscriptionUtils.getIsoCodeTupleList must contain(("US" , "USA :United States of America"))
        AtedSubscriptionUtils.getIsoCodeTupleList must contain(("GB" , "United Kingdom :UK, GB, Great Britain"))
        AtedSubscriptionUtils.getIsoCodeTupleList must contain(("UG" , "Uganda"))
      }
    }

    "format Post Code" must {
      "return None if we have no post code" in {
        AtedSubscriptionUtils.formatPostCode(None) must be(None)
      }

      "add a space into the correct position when the post code is 6 characters" in {
        AtedSubscriptionUtils.formatPostCode(Some("AA1 1AA")) must be(Some("AA1 1AA"))
        AtedSubscriptionUtils.formatPostCode(Some("AA11AA")) must be(Some("AA1 1AA"))
        AtedSubscriptionUtils.formatPostCode(Some("  AA11AA    ")) must be(Some("AA1 1AA"))
      }
      "add a space into the correct position when the post code is 7 characters" in {
        AtedSubscriptionUtils.formatPostCode(Some("AA11 1AA")) must be (Some("AA11 1AA"))
        AtedSubscriptionUtils.formatPostCode(Some("AA111AA")) must be (Some("AA11 1AA"))
        AtedSubscriptionUtils.formatPostCode(Some("   AA 111AA    ")) must be (Some("AA11 1AA"))
      }
      "the case should be correct for the postcode" in {
        AtedSubscriptionUtils.formatPostCode(Some("aa1 1aa")) must be(Some("AA1 1AA"))
      }
    }


  }

}
