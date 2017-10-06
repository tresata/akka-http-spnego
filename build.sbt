lazy val sharedSettings = Seq(
  organization := "com.tresata",
  version := "0.3.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8", "-feature", "-language:_", "-Xlint:-package-object-classes,-adapted-args,_",
    "-Ywarn-unused-import", "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused"),
  scalacOptions in (Test, compile) := (scalacOptions in (Test, compile)).value.filter(_ != "-Ywarn-value-discard").filter(_ != "-Ywarn-unused"),
  scalacOptions in (Compile, console) := (scalacOptions in (Compile, console)).value.filter(_ != "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Test, console)).value.filter(_ != "-Ywarn-unused-import"),
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  publishArtifact in Test := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".m2" / "credentials_sonatype"),
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
    "org.slf4j" % "slf4j-api" % "1.7.5" % "compile",
    "com.typesafe.akka" %% "akka-http" % "10.0.9" % "compile",
    "commons-codec" % "commons-codec" % "1.4" % "compile",
    "org.scalatest" %% "scalatest" % "3.0.3" % "test"
  )
)

lazy val `test-server` = (project in file("test-server")).settings(
  sharedSettings
).settings(
  name := "test-server",
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "compile",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.17" % "compile"
  ),
  publish := { },
  publishLocal := { }
).dependsOn(`akka-http-spnego`)
