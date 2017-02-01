// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

// ScalikeJDBC

libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212.jre7"

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "2.5.0")
