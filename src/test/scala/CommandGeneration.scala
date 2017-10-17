package ohnosequences.flash.test

import org.scalatest.FunSuite
import ohnosequences.flash._, api._
import ohnosequences.cosas._, types._, klists._
import java.io._

class CommandGeneration extends FunSuite {

  test("command generation for Flash expressions") {

    val flashExpr = flash(
      input(FlashInputAt(
        new File("reads1.fastq"),
        new File("reads2.fastq")
      )) ::
      output(FlashOutputAt(
        new File("/tmp/out"),
        "sample1"
      )) ::
      *[AnyDenotation],
      flash.defaults.update(
        allow_outies(true) ::
        *[AnyDenotation]
      ).value
    )

    assert {
      flashExpr.toSeq === Seq(
        "flash",
        new File("reads1.fastq").getCanonicalPath,
        new File("reads2.fastq").getCanonicalPath,
        "--output-prefix", "sample1",
        "--output-directory", new File("/tmp/out").getCanonicalPath,
        "--min-overlap", "10",
        "--max-overlap", "65",
        "--read-len", "100",
        "--fragment-len", "180",
        "--fragment-len-stddev", "18",
        "--threads", "1",
        "--allow-outies",
        "--phred-offset", "33",
        "--cap-mismatch-quals"
      )
    }
  }
}
