publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <scm>
    <url>git@github.com:lifeway/play-dynamodb.git</url>
    <connection>scm:git:git@github.com:lifeway/play-dynamodb.git</connection>
  </scm>
    <developers>
      <developer>
        <id>lifeway</id>
        <name>LifeWay Christian Resources</name>
        <url>https://www.lifeway.com</url>
      </developer>
    </developers>
}
pomIncludeRepository := { _ => false }
homepage := Some(url(s"https://github.com/lifeway/play-dynamodb"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

sonatypeProfileName := "com.lifeway"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseTagName := s"${(version in ThisBuild).value}"
releaseCrossBuild := true

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
