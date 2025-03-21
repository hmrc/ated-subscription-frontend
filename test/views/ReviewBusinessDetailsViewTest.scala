/*
 * Copyright 2025 HM Revenue & Customs
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

package views

import play.twirl.api.HtmlFormat
import utils.{AtedSubscriptionUtils, AtedSubscriptionUtilsImpl}
import views.html.reviewBusinessDetails

class ReviewBusinessDetailsViewTest extends ViewTestFixture{
  val backLinkUrl: Option[String] = Some("test-url")
  val mockAtedSubUtils: AtedSubscriptionUtils = new AtedSubscriptionUtilsImpl(app.environment)

  val view: reviewBusinessDetails = app.injector.instanceOf[views.html.reviewBusinessDetails]
  override val htmlContent: HtmlFormat.Appendable =
    view.apply(testReviewBusinessDetails,testAddress,testContact,Some(testContactEmail),Some(emailAddress),
      Some(clientDisplayName),backLinkUrl)(fakeRequest, authContextAgent, messages, mockAtedSubUtils, mockAppConfig)

  "registeredBusinessAddress page" should {
    "render the correct content" in {
      heading mustBe "This section is: Add a client Check your client’s ATED details are correct"
      buttonText mustBe "Confirm and continue"
      back_link mustBe "Back"
      back_link_href mustBe "test-url"
    }
  }

}
