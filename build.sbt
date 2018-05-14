name          := "flash-api"
organization  := "ohnosequences"
description   := "A typesafe Scala API for FLASh"
bucketSuffix  := "era7.com"

crossScalaVersions := Seq("2.11.12", "2.12.6")
scalaVersion := crossScalaVersions.value.max

libraryDependencies ++= Seq(
  "ohnosequences" %% "cosas"     % "0.10.1",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

// NOTE should be reestablished
wartremoverErrors in (Test, compile) := Seq()
wartremoverErrors in (Compile, compile) := Seq()
