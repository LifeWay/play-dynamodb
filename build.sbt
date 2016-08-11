name := "Play-DynamoDB"
organization := "com.lifeway"
scalaVersion := "2.11.8"

resolvers += "Kaliber Internal Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"

scalafmtConfig in ThisBuild := Some(file(".scalafmt"))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.0" % "provided",
  "com.typesafe.play" %% "play-ws" % "2.5.0" % "provided",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.+" % "provided",
  "net.kaliber" %% "play-s3" % "8.0.0" % "provided",
  "com.typesafe.play" %% "play-test" % "2.5.0" % "test",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.21" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

addCommandAlias("testfull", "; clean; compile; test; scalafmtTest")