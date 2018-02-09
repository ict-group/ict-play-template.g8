name := "$name$"

organization := "$organization$"

version := "$version$"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin)

scalaVersion := "2.12.3"

val postgresql = "org.postgresql" % "postgresql" % "42.2.0"
val mssql = "com.microsoft.sqlserver" % "mssql-jdbc" % "6.1.0.jre8"


val akkaVersion = "2.5.6"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  filters,
  postgresql,
  mssql,
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.1",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "com.pauldijou" %% "jwt-play" % "0.13.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

libraryDependencies += guice
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.2"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.2"

libraryDependencies += "com.iheart" %% "play-swagger" % "0.6.1-PLAY2.6"
libraryDependencies += "org.webjars" % "swagger-ui" % "2.2.0"

resolvers += "Artifactory" at "http://artifactory.ict-group.it:8081/artifactory/sbt-remote/"

import com.typesafe.sbt.packager.docker._

packageName in Docker := "gitlab.ict-group.it:4567/ict/a-b-normal"

dockerBaseImage := "java:8-jre-alpine"

dockerCommands += Cmd("USER", "root")
dockerCommands += Cmd("RUN", "apk add --no-cache bash tzdata")
dockerCommands += Cmd("RUN", "echo \"Europe/Rome\" >  /etc/timezone")
dockerCommands += Cmd("RUN", "cp /usr/share/zoneinfo/Europe/Rome /etc/localtime")
dockerCommands += Cmd("USER", "daemon")

dockerExposedPorts := Seq(9000, 9443)
