
```scala
package ohnosequences.flash.test

import org.scalatest.FunSuite

import ohnosequences.flash._, api._

import better.files._
import ohnosequences.cosas._, types._, klists._

class CommandGeneration extends FunSuite {

  test("command generation for Flash expressions") {

    val flashExpr = FlashExpression(
      flash.arguments(
        (input   := FlashInputAt(File("reads1.fastq"), File("reads2.fastq")) ) ::
        (output  := FlashOutputAt(File("/tmp/out"), "sample1")               ) :: *[AnyDenotation]
      ),
      flash.defaults update (allow_outies(true) :: *[AnyDenotation])
    )

    assert {
      flashExpr.cmd === Seq(
        "flash",
        File("reads1.fastq").path.toString,
        File("reads2.fastq").path.toString,
        "--output-prefix", "sample1",
        "--output-directory", File("/tmp/out").path.toString,
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

```




[test/scala/CommandGeneration.scala]: CommandGeneration.scala.md
[test/scala/ParseMergeStats.scala]: ParseMergeStats.scala.md
[main/scala/api.scala]: ../../main/scala/api.scala.md
[main/scala/data.scala]: ../../main/scala/data.scala.md