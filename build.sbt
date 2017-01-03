lazy val sharedSettings = Seq(
  organization := "com.tresata",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.7", "-feature", "-language:_", "-Xlint:-package-object-classes,-adapted-args,_",
    "-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused"),
  scalacOptions in (Test, compile) := (scalacOptions in (Test, compile)).value.filter(_ != "-Ywarn-value-discard").filter(_ != "-Ywarn-unused"),
  scalacOptions in (Compile, console) := (scalacOptions in (Compile, console)).value.filter(_ != "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Test, console)).value.filter(_ != "-Ywarn-unused-import"),
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  publishArtifact in Test := false
)

lazy val `akka-http-spnego` = (project in file(".")).settings(
  sharedSettings
).settings(
  name := "akka-http-spnego",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.5" % "compile",
    "com.typesafe.akka" %% "akka-http" % "10.0.1" % "compile",
    "commons-codec" % "commons-codec" % "1.4" % "compile",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )
)

lazy val `test-server` = (project in file("test-server")).settings(
  sharedSettings
).settings(
  name := "test-server",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "compile",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.16" % "compile"
  ),
  publish := { },
  publishLocal := { }
).dependsOn(`akka-http-spnego`)
