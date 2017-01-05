name := "$name$"

organization := "$organization$"

version := "$version$"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin)

scalaVersion := "2.11.8"

val postgresql = "org.postgresql" % "postgresql" % "9.4.1211.jre7"

lazy val scalikejdbcPlayVersion = "2.5.+"

val akkaVersion = "2.4.4"

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  evolutions,
  postgresql,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.2",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "org.scalikejdbc" %% "scalikejdbc"                  % scalikejdbcPlayVersion,
  "org.scalikejdbc" %% "scalikejdbc-config"           % scalikejdbcPlayVersion,
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % scalikejdbcPlayVersion,
  "org.scalikejdbc" %% "scalikejdbc-play-fixture"     % scalikejdbcPlayVersion,
  "org.scalikejdbc" %% "scalikejdbc-play-dbapi-adapter" % scalikejdbcPlayVersion,
  "com.typesafe.play.modules" %% "play-modules-redis" % "2.5.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

scalikejdbcSettings

libraryDependencies += "com.iheart" %% "play-swagger" % "0.5.2"
libraryDependencies += "org.webjars" % "swagger-ui" % "2.2.0"

publishMavenStyle := true

resolvers += "Artifactory" at "http://artifactory.ict-group.it:8081/artifactory/sbt-remote/"

import com.typesafe.sbt.packager.docker._

dockerBaseImage := "java:8-jre-alpine"

dockerCommands += Cmd("USER", "root")
dockerCommands += Cmd("RUN", "apk add --no-cache bash")
dockerCommands += Cmd("USER", "daemon")

dockerExposedPorts := Seq(9000, 9443)
