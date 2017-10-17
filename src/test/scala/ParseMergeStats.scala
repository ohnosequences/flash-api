package ohnosequences.flash.test

import org.scalatest.FunSuite
import ohnosequences.flash._, api._
import java.io._

class ParseMergeStats extends FunSuite {

  test("can get mergeStats record values from output file") {

    val output = FlashOutputAt(
      outputPath = new File("."),
      prefix = "out"
    )

    lazy val readStats = output stats

    val mergedReadsTotal = readStats.foldLeft(0){
      (accum, next) => next.fold(l => 0, v => accum + (v get FlashOutput.readNumber).value )
    }

    assert { mergedReadsTotal === 96479 }
  }

}
