
```scala
package ohnosequences.flash.test

import org.scalatest.FunSuite

import ohnosequences.flash._, api._

import java.io.File
import ohnosequences.cosas._, typeSets._

class CommandGeneration extends FunSuite {

  test("command generation for Flash expressions") {

    val flashExpr = FlashExpression(flash)(
      flash.arguments(
        input( FlashInputAt(new File("reads1.fastq"), new File("reads2.fastq")) ) :~:
        output( FlashOutputAt(new File("/tmp/out"), "sample1") )                  :~: ∅
      ),
      flash.defaults update (allow_outies(true) :~: ∅)
    )

    assert {
      flashExpr.cmd === Seq(
        "flash",
        (new File("reads1.fastq")).getCanonicalPath.toString,
        (new File("reads2.fastq")).getCanonicalPath.toString,
        "--output-prefix", "sample1",
        "--output-directory", (new File("/tmp/out")).getCanonicalPath.toString,
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




[main/scala/api.scala]: ../../main/scala/api.scala.md
[main/scala/data.scala]: ../../main/scala/data.scala.md
[test/scala/CommandGeneration.scala]: CommandGeneration.scala.md
[test/scala/ParseMergeStats.scala]: ParseMergeStats.scala.md