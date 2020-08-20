package helpers.application

import helpers.wiremock.WireMockConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.http.HttpClient
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import utils.{AtedSubscriptionUtils, AtedSubscriptionUtilsImpl}
import play.api.inject.{bind => playBind}

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "ated-subscription-frontend"
  val testAppUrl: String        = s"http://localhost:$port/ated-subscription"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  val appConfig: Map[String, Any] = Map(
    "application.router"                                  -> "testOnlyDoNotUseInAppConf.Routes",
    "mongo.uri"                                           -> "mongodb://localhost:27017/test-ated-subscription-frontend",
    "microservice.metrics.graphite.host"                  -> "localhost",
    "microservice.metrics.graphite.port"                  -> 2003,
    "microservice.metrics.graphite.prefix"                -> "play.ated-subscription-frontend.",
    "microservice.metrics.graphite.enabled"               -> true,
    "microservice.services.auth.host"                     -> wireMockHost,
    "microservice.services.auth.port"                     -> wireMockPort,
    "microservice.services.business-customer-frontend.host"    -> wireMockHost,
    "microservice.services.business-customer-frontend.port"    -> wireMockPort,
    "microservice.services.agent-client-mandate-frontend.host"    -> wireMockHost,
    "microservice.services.agent-client-mandate-frontend.port"    -> wireMockPort,
    "microservice.services.tax-enrolments.host"            -> wireMockHost,
    "microservice.services.tax-enrolments.port"            -> wireMockPort,
    "microservice.services.ated-subscription.host"         -> wireMockHost,
    "microservice.services.ated-subscription.port"         -> wireMockPort,
    "microservice.services.session-cache.host"            -> wireMockHost,
    "microservice.services.session-cache.port"            -> wireMockPort,
    "microservice.services.cachable.session-cache.host"   -> wireMockHost,
    "microservice.services.cachable.session-cache.port"   -> wireMockPort,
    "microservice.services.etmp-hod.host"                 -> wireMockHost,
    "microservice.services.etmp-hod.port"                 -> wireMockPort,
    "microservice.services.auth.company-auth.host"        -> wireMockHost,
    "metrics.name"                                        -> "ated-subscription-frontend",
    "metrics.rateUnit"                                    -> "SECONDS",
    "metrics.durationUnit"                                -> "SECONDS",
    "metrics.showSamples"                                 -> true,
    "metrics.jvm"                                         -> true,
    "metrics.enabled"                                     -> true
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(playBind[HttpClient].to[DefaultHttpClient])
    .overrides(playBind[AtedSubscriptionUtils].to[AtedSubscriptionUtilsImpl])
    .build()
}
