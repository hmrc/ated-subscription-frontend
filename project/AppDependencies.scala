import sbt._

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.2.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "1.14.0-play-27",
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "domain" % "5.11.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-27"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.jsoup" % "jsoup" % "1.13.1" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
        "org.mockito" % "mockito-core" % "3.10.0" % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.28.0" % "test,it"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}


