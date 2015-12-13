Nice.scalaProject

name          := "flash"
organization  := "ohnosequences"
description   := "A typesafe Scala API for FLASh"

bucketSuffix  := "era7.com"

libraryDependencies ++= Seq(
  "ohnosequences"         %% "cosas"        % "0.8.0",
  "ohnosequences"         %% "datasets"     % "0.2.0",
  "com.github.pathikrit"  %% "better-files" % "2.13.0",
  "com.github.tototoshi"  %% "scala-csv"    % "1.2.2",
  "org.scalatest"         %% "scalatest"    % "2.2.5" % Test
)
