name := "fs2-jms"
organization := "io.github.kiambogo"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-deprecation", // warn about use of deprecated APIs
  "-unchecked", // warn about unchecked type parameters
  "-feature", // warn about misused language features
  "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
  "-language:implicitConversions", // allow use of implicit conversions
  "-Xfatal-warnings", // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

val fs2Version = "1.0.2"

libraryDependencies ++= Seq(
  "co.fs2"           %% "fs2-core"          % fs2Version,
  "co.fs2"           %% "fs2-io"            % fs2Version,
  "javax.jms"        % "javax.jms-api"      % "2.0.1",
  "org.scalatest"    %% "scalatest"         % "3.0.4" % Test,
  "org.mockito"      % "mockito-scala_2.12" % "1.0.0" % Test,
  "io.github.sullis" %% "jms-testkit"       % "0.0.10" % Test
)

// coverage
coverageMinimum := 60
coverageFailOnMinimum := true

publishTo := Some("Sonatype Nexus" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

licenses := Seq("MIT" -> url("https://github.com/kiambogo/fs2-jms/blob/master/LICENSE"))

developers := List(
  Developer(id = "kiambogo", name = "Christopher Poenaru", email = "kiambogo@gmail.com", url = url("https://github.com/kiambogo"))
)
homepage := Some(url("https://github.com/kiambogo/fs2-jms"))
scmInfo := Some(ScmInfo(url("https://github.com/kiambogo/fs2-jms"), "scm:git:git@github.com:kiambogo/fs2-jms.git"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseCommitMessage := s"[skip travis] Setting version to ${(version in ThisBuild).value}"
