
import TestPhases.*
import sbt.Keys.*
import sbt.{Def, inConfig, *}
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ated-subscription-frontend"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.12"

lazy val appDependencies : Seq[ModuleID] = AppDependencies()
lazy val playSettings: Seq[Setting[?]] = Seq.empty
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings: Seq[Def.Setting[? >: String & Double & Boolean]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;views.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq( play.sbt.PlayScala, SbtDistributablesPlugin ) *)
  .settings(playSettings *)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    scalacOptions ++= Seq("-Wconf:src=target/.*:s", "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s", "-Ywarn-unused:-explicits,-implicits"),
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    Test / parallelExecution := false,
    Test / fork := true
  )
  .settings(inConfig(TemplateTest )(Defaults.testSettings) *)
  .settings(

    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.govukfrontend.views.html.components._"
    )
  )
  .settings(
    resolvers += Resolver.jcenterRepo
  )
  .disablePlugins(JUnitXmlReportPlugin)