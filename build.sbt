name          := "flash-api"
organization  := "ohnosequences"
description   := "A typesafe Scala API for FLASh"
bucketSuffix  := "era7.com"

crossScalaVersions := Seq("2.11.11", "2.12.3")
scalaVersion := crossScalaVersions.value.max

libraryDependencies ++= Seq(
  "ohnosequences" %% "cosas"     % "0.10.0",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

// NOTE should be reestablished
wartremoverErrors in (Test, compile) := Seq()
wartremoverErrors in (Compile, compile) := Seq()
