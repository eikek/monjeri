import de.johoop.testngplugin.TestNGPlugin._
testNGSettings

name := "monjeri"
organization := "org.monjeri"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.1"
autoScalaLibrary := false
crossPaths := false

testNGVersion := "6.11"

javacOptions ++= Seq(
  "-Xlint:unchecked",
  "-source", "1.8",
  "-target", "1.8",
  "-deprecation"
)

libraryDependencies ++= Seq(
  "org.mongodb" % "mongodb-driver" % "3.4.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.testng" % "testng" % testNGVersion.value % "test",
  "org.slf4j" % "jul-to-slf4j" % "1.7.25" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.2" % "test",
  "com.lihaoyi" % "ammonite" % "0.8.2" % "test" cross CrossVersion.full
)
