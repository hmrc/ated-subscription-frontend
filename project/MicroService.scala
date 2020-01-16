import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.{Def, inConfig, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc._
  import uk.gov.hmrc._
  import TestPhases.oneForkedJvmPerTest
  import DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName: String

  lazy val appDependencies: Seq[ModuleID] = ???
  lazy val plugins: Seq[Plugins] = Seq.empty
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings: Seq[Def.Setting[_ >: String with Double with Boolean]] = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;views.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
      ScoverageKeys.coverageMinimum := 90,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings( majorVersion := 2 )
    .configs(IntegrationTest)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(scalaVersion := "2.11.11")
    .settings(playSettings ++ scoverageSettings: _*)
    .settings(
      addTestReportOption(IntegrationTest, "int-test-reports"),
      inConfig(IntegrationTest)(Defaults.itSettings),
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := InjectedRoutesGenerator,
      Keys.fork                  in Test            := true,
      Keys.fork                  in IntegrationTest :=  false,
      unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      testGrouping               in IntegrationTest :=  oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )
}
