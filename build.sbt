name := """play-scala-macwire-di-example"""

version := "2.7.x"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

val akkaVersion: String = sys.props.getOrElse("akka.version", "2.5.23")

libraryDependencies += "com.softwaremill.macwire" %% "macros"                      % "2.3.2" % "provided"
libraryDependencies += "com.typesafe.akka"        %% "akka-cluster-sharding-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka"        %% "akka-persistence-typed"      % akkaVersion
libraryDependencies += "com.github.dnvriend"      %% "akka-persistence-jdbc"       % "3.5.2"
libraryDependencies += "org.postgresql"           % "postgresql"                   % "42.2.5"

libraryDependencies ++=
  Seq(
    "specs2-core",
    "specs2-junit",
    "specs2-matcher-extra"
  ).map("org.specs2" %% _ % "4.7.0" % Test)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)
