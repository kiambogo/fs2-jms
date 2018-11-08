name := "fs2-jms"

scalaVersion := "2.12.7"

val fs2Version = "1.0.0"

externalResolvers := ("jboss" at "http://repository.jboss.org/nexus/content/groups/public") +: externalResolvers.value

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io"   % fs2Version,
  // "javax.jms" % "jms"           % "1.1",
  "javax.jms" % "javax.jms-api" % "2.0.1",
  "org.scalatest" %% "scalatest"                    % "3.0.4" % Test,
  "com.ibm.mq" % "com.ibm.mq.allclient" % "9.0.4.0"
)
