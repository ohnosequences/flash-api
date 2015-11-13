Nice.scalaProject

name          := "flash"
organization  := "ohnosequences"
description   := "A typesafe Scala API for FLASh"

bucketSuffix  := "era7.com"

libraryDependencies ++= Seq(
  "ohnosequences"         %% "cosas"      % "0.8.0-SNAPSHOT",
  "ohnosequences"         %% "datasets"   % "0.2.0-SNAPSHOT",
  "com.github.tototoshi"  %% "scala-csv"  % "1.2.2",
  "org.scalatest"         %% "scalatest"  % "2.2.5" % Test
)
