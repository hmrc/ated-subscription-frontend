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
import views.html.inform

class InformViewTest extends ViewTestFixture{

  val backLinkUrl: Option[String] = Some("test-url")

  val view: inform = app.injector.instanceOf[views.html.inform]
  override val htmlContent: HtmlFormat.Appendable =
    view.apply(backLinkUrl)(fakeRequest, authContextOrg, messages, mockAppConfig)

  "Inform view" should {
    "render the correct content" in {
      heading mustBe "This section is: ATED registration Inform HMRC as soon as you create a new ATED account"
      buttonText mustBe "Continue to create new account"
      back_link mustBe "Back"
      back_link_href mustBe "test-url"
    }
  }
}
