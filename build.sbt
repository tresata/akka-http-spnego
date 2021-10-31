lazy val sharedSettings = Seq(
  organization := "com.tresata",
  version := "0.6.0-SNAPSHOT",
  licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"),
  scalaVersion := "2.12.15",
  crossScalaVersions := Seq("2.12.15", "2.13.5"),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8", "-feature", "-language:_", "-Xlint:-package-object-classes,-adapted-args,_",
    "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused"),
  Test / compile / scalacOptions := (Test / compile / scalacOptions).value.filter(_ != "-Ywarn-value-discard").filter(_ != "-Ywarn-unused"),
  Compile / console / scalacOptions := (Compile / console / scalacOptions).value.filter(_ != "-Ywarn-unused-import"),
  Test / console / scalacOptions := (Test / console / scalacOptions).value.filter(_ != "-Ywarn-unused-import"),
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  Test / publishArtifact := false,
  publishTo := {
    if (isSnapshot.value)
      Some("snapshots" at "https://server02.tresata.com:8084/artifactory/oss-libs-snapshot-local")
    else
      Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".m2" / "credentials_sonatype"),
  credentials += Credentials(Path.userHome / ".m2" / "credentials_artifactory"),
  pomExtra := (
    <url>https://github.com/tresata/akka-http-spnego</url>
    <scm>
      <url>git@github.com:tresata/akka-http-spnego.git</url>
      <connection>scm:git:git@github.com:tresata/akka-http-spnego.git</connection>
    </scm>
    <developers>
      <developer>
        <id>koertkuipers</id>
        <name>Koert Kuipers</name>
        <url>https://github.com/koertkuipers</url>
      </developer>
    </developers>
  )
)

lazy val `akka-http-spnego` = (project in file(".")).settings(
  sharedSettings
).settings(
  name := "akka-http-spnego",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25" % "compile",
    "com.typesafe.akka" %% "akka-http" % "10.2.6" % "compile",
    "com.typesafe.akka" %% "akka-stream" % "2.6.16" % "compile",
    "commons-codec" % "commons-codec" % "1.15" % "compile",
    "org.scalatest" %% "scalatest-funspec" % "3.2.10" % "test"
  )
)

lazy val `test-server` = (project in file("test-server")).settings(
  sharedSettings
).settings(
  name := "test-server",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.25" % "compile",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.16" % "compile"
  ),
  publish := { },
  publishLocal := { }
).dependsOn(`akka-http-spnego`)
