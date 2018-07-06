import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "ated-subscription-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val frontendBootstrapVersion = "8.24.0"
  private val httpCachingClientVersion = "7.0.0"
  private val playPartialsVersion = "6.1.0"
  private val domainVersion = "4.1.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2",
    "uk.gov.hmrc" %% "auth-client" % "2.4.0"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.jsoup" % "jsoup" % "1.9.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % "2.3.0",
        "org.mockito" % "mockito-all" % "1.10.19" % scope
      )
    }.test

  }

  def apply() = compile ++ Test()
}


