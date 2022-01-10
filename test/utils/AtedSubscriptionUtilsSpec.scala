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

package utils

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class AtedSubscriptionUtilsSpec extends PlaySpec with GuiceOneServerPerSuite {

  trait Setup {
    val atedSubUtil = new AtedSubscriptionUtilsImpl(app.environment)
  }

  "AtedSubscriptionUtils" must {

    "getSelectedCountry" must {
      "bring the correct country from the file" in new Setup {
        atedSubUtil.getSelectedCountry("GB") must be("United Kingdom")
        atedSubUtil.getSelectedCountry("US") must be("USA")
        atedSubUtil.getSelectedCountry("VG") must be("British Virgin Islands")
        atedSubUtil.getSelectedCountry("UG") must be("Uganda")
        atedSubUtil.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in new Setup {
        atedSubUtil.getIsoCodeTupleList must contain(("US" , "USA :United States of America"))
        atedSubUtil.getIsoCodeTupleList must contain(("GB" , "United Kingdom :UK, GB, Great Britain"))
        atedSubUtil.getIsoCodeTupleList must contain(("UG" , "Uganda"))
      }
    }

    "format Post Code" must {
      "return None if we have no post code" in new Setup {
        atedSubUtil.formatPostCode(None) must be(None)
      }

      "add a space into the correct position when the post code is 6 characters" in new Setup {
        atedSubUtil.formatPostCode(Some("AA1 1AA")) must be(Some("AA1 1AA"))
        atedSubUtil.formatPostCode(Some("AA11AA")) must be(Some("AA1 1AA"))
        atedSubUtil.formatPostCode(Some("  AA11AA    ")) must be(Some("AA1 1AA"))
      }
      "add a space into the correct position when the post code is 7 characters" in new Setup {
        atedSubUtil.formatPostCode(Some("AA11 1AA")) must be (Some("AA11 1AA"))
        atedSubUtil.formatPostCode(Some("AA111AA")) must be (Some("AA11 1AA"))
        atedSubUtil.formatPostCode(Some("   AA 111AA    ")) must be (Some("AA11 1AA"))
      }
      "the case should be correct for the postcode" in new Setup {
        atedSubUtil.formatPostCode(Some("aa1 1aa")) must be(Some("AA1 1AA"))
      }
    }


    "validateGroupId" must {

      "throw an exception" when {
        "invalid string is passed" in new Setup {
          val thrown: RuntimeException = the[RuntimeException] thrownBy atedSubUtil.validateGroupId("abc-def-ghi")
          thrown.getMessage must include("Invalid groupId from auth")
        }
      }

      "return groupId" when {
        "valid string is passed" in new Setup {
          val result: String = atedSubUtil.validateGroupId("42424200-0000-0000-0000-000000000000")
          result must be("42424200-0000-0000-0000-000000000000")
        }

        "string with testGroupId- is passed" in new Setup {
          val result: String = atedSubUtil.validateGroupId("testGroupId-0000-0000-0000-000000000000")
          result must be("0000-0000-0000-000000000000")
        }
      }

    }


  }

}
