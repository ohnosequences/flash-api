package ohnosequences.flash.test

import org.scalatest.FunSuite

import ohnosequences.flash._, api._

import java.io.File
import ohnosequences.cosas._, typeSets._, types._

class ParseMergeStats extends FunSuite {

  test("can get mergeStats record values from output file") {

    val output = FlashOutputAt(
      outputPath = new File("."),
      prefix = "out"
    )

    lazy val readStats = output stats

    val mergedReadsTotal = readStats.foldLeft(0){
      (accum, next) => next.fold(l => 0, v => accum + (v get readNumber).value )
    }
    
    println(s"total number of merged reads: ${mergedReadsTotal}")

    readStats map { errOrV =>
      errOrV.fold(
        { err => println(s"oh, an error: ${err}") },
        { b: ValueOf[mergedStats.type] => println(s"${b get mergedReadLength show} :~: ${b get readNumber show}") }
      )
    }
  }

}
