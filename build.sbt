name := "fs2-jms"
organization := "io.github.kiambogo"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-deprecation", // warn about use of deprecated APIs
  "-unchecked", // warn about unchecked type parameters
  "-feature", // warn about misused language features
  "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
  "-language:implicitConversions", // allow use of implicit conversions
  "-Xfatal-warnings", // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

val fs2Version = "1.0.0"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-core"          % fs2Version,
  "co.fs2"        %% "fs2-io"            % fs2Version,
  "javax.jms"     % "javax.jms-api"      % "2.0.1",
  "org.scalatest" %% "scalatest"         % "3.0.4" % Test,
  "org.mockito"   % "mockito-scala_2.12" % "1.0.0" % Test
)

// coverage
coverageMinimum := 60
coverageFailOnMinimum := true
