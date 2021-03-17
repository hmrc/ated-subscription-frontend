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

package config

import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi, Messages}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

class ErrorHandler @Inject()(val messagesApi: MessagesApi,
                             val configuration: Configuration,
                             implicit val appConfig: ApplicationConfig,
														 val templateError: views.html.global_error
                            ) extends FrontendErrorHandler with I18nSupport {

	override def standardErrorTemplate(pageTitle: String, heading: String, message: String)
																		(implicit request: Request[_]): Html = {
		templateError(pageTitle, heading, message)
	}

	override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
		templateError(
			Messages("ated.business-registration.generic.error.header"),
			Messages("ated.business-registration.generic.error.title"),
			Messages("ated.business-registration.generic.error.message")
		)
	}

}
