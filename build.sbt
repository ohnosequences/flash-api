Nice.scalaProject

name          := "flash"
organization  := "ohnosequences"
description   := "A typesafe Scala API for FLASh"

bucketSuffix  := "era7.com"

libraryDependencies ++= Seq(
  "ohnosequences" %% "cosas"      % "0.7.0",
  "org.scalatest" %% "scalatest"  % "2.2.5" % Test
)
