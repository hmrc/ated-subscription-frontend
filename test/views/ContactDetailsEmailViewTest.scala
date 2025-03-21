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

import forms.AtedForms.contactDetailsEmailForm
import play.twirl.api.HtmlFormat
import views.html.contactDetailsEmail

class ContactDetailsEmailViewTest extends ViewTestFixture{

  val backLinkUrl: Option[String] = Some("test-url")


  val view: contactDetailsEmail = app.injector.instanceOf[views.html.contactDetailsEmail]
  override val htmlContent: HtmlFormat.Appendable =
    view.apply(contactDetailsEmailForm,mode,backLinkUrl)(fakeRequest, authContextAgent, messages, mockAppConfig)

  "registeredBusinessAddress page" should {
    "render the correct content" in {
      heading mustBe "This section is: Add a client Can we use an email address as a point of contact?"
      buttonText mustBe "Continue"
      back_link mustBe "Back"
      back_link_href mustBe "test-url"
    }
  }
}
