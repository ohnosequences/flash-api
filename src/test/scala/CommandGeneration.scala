package ohnosequences.flash.test

import org.scalatest.FunSuite

import ohnosequences.flash._, api._

import java.io.File
import ohnosequences.cosas._, typeSets._

class CommandGeneration extends FunSuite {

  test("command generation for arguments") {

    val uh = flash.arguments(
      input( FlashInputAt(new File("reads1.fastq"), new File("reads2.fastq")) ) :~:
      output( FlashOutputAt(new File("/tmp/out"), "sample1") )                  :~: ∅
    )

    val uhoh = uh.value mapToList optionValueToSeq

    println(uhoh.flatten)

    val opts = flash.defaults

    val optsSeqs = opts.value mapToList optionValueToSeq

    println(optsSeqs.flatten)
  }
}
