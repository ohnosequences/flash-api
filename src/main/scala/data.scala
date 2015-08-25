package ohnosequences.flash

import ohnosequences.datasets._, dataSets._
import api._

case object data {

  case object MergedReadStatsType extends AnyDataType {

    type Record = mergedStats.type
    val record: Record = mergedStats

    val label = toString
  }

  trait AnyMergedReadStats extends AnyData {

    type DataType = MergedReadStatsType.type
    val dataType = MergedReadStatsType
  }
}
