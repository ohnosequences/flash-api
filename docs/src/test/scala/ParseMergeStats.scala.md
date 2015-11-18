
```scala
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

    assert { mergedReadsTotal === 96479 }
  }

}

```




[main/scala/api.scala]: ../../main/scala/api.scala.md
[main/scala/data.scala]: ../../main/scala/data.scala.md
[test/scala/CommandGeneration.scala]: CommandGeneration.scala.md
[test/scala/ParseMergeStats.scala]: ParseMergeStats.scala.md