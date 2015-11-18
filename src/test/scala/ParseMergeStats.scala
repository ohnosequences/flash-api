package ohnosequences.flash.test

import org.scalatest.FunSuite

import ohnosequences.flash._, api._

import better.files._
import ohnosequences.cosas._, types._, records._

class ParseMergeStats extends FunSuite {

  test("can get mergeStats record values from output file") {

    val output = FlashOutputAt(
      outputPath = File("."),
      prefix = "out"
    )

    lazy val readStats = output stats

    val mergedReadsTotal = readStats.foldLeft(0){
      (accum, next) => next.fold(l => 0, v => accum + (v get readNumber).value )
    }

    assert { mergedReadsTotal === 96479 }
  }

}
