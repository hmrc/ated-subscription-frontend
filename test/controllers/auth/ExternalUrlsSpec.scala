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

package controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ExternalUrlsSpec extends PlaySpec with OneServerPerSuite {

  "ExternalUrls" must {

    "have companyAuthHost " in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }

    "have loginCallback " in {
      ExternalUrls.loginCallback must be("http://localhost:9933/ated-subscription/start-subscription")
    }

    "have loginPath " in {
      ExternalUrls.loginPath must be("sign-in")
    }

    "have signIn " in {
      ExternalUrls.signIn must be(s"""http://localhost:9025/gg/sign-in?continue=http://localhost:9933/ated-subscription/start-subscription""")
    }

    "have loginURL " in {
      ExternalUrls.loginURL must be(s"""http://localhost:9025/gg/sign-in""")
    }

    "have logout " in {
      ExternalUrls.logoutPath must be(s"""http://localhost:9916/ated/logout""")
    }

    "have client summary " in {
      ExternalUrls.clientSummaryPath must be(s"""http://localhost:9916/ated/summary""")
    }

    "have review details path" in {
      ExternalUrls.reviewDetailsPath must be(s"""http://localhost:9923/business-customer/review-details/ATED""")
    }

    "have back to customer link path" in {
      ExternalUrls.backToBusinessCustomerUrl must be(s"""http://localhost:9923/business-customer/back-link/ATED""")
    }
  }
  
}
