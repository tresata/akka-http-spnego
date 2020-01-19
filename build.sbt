lazy val sharedSettings = Seq(
  organization := "com.tresata",
  version := "0.6.0-SNAPSHOT",
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1"),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8", "-feature", "-language:_", "-Xlint:-package-object-classes,-adapted-args,_",
    "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused"),
  scalacOptions in (Test, compile) := (scalacOptions in (Test, compile)).value.filter(_ != "-Ywarn-value-discard").filter(_ != "-Ywarn-unused"),
  scalacOptions in (Compile, console) := (scalacOptions in (Compile, console)).value.filter(_ != "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Test, console)).value.filter(_ != "-Ywarn-unused-import"),
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  publishArtifact in Test := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at "http://server02.tresata.com:8081/artifactory/oss-libs-snapshot-local")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".m2" / "credentials_sonatype"),
  credentials += Credentials(Path.userHome / ".m2" / "credentials_artifactory"),
  pomExtra := (
    <url>https://github.com/tresata/akka-http-spnego</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>  
        <distribution>repo</distribution>
        <comments>A business-friendly OSS license</comments>
      </license>
    </licenses>
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
    "com.typesafe.akka" %% "akka-http" % "10.1.9" % "compile",
    "com.typesafe.akka" %% "akka-stream" % "2.5.23" % "compile",
    "commons-codec" % "commons-codec" % "1.10" % "compile",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test"
  )
)

lazy val `test-server` = (project in file("test-server")).settings(
  sharedSettings
).settings(
  name := "test-server",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.25" % "compile",
    "com.typesafe.akka" %% "akka-slf4j" % "2.5.23" % "compile"
  ),
  publish := { },
  publishLocal := { }
).dependsOn(`akka-http-spnego`)
