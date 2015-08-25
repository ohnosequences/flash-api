package ohnosequences.flash

import ohnosequences.datasets._, dataSets._
import api._

case object data {

  // data types
  case object MergedReadStatsType extends AnyDataType {

    type Record = mergedStats.type
    val record: Record = mergedStats

    val label = toString
  }

  // In a future world this could link to the illumina read type
  trait AnyMergedReadsType extends AnyDataType

  // data
  trait AnyMergedReads extends AnyData {

    type DataType <: AnyMergedReadsType

    type FlashExpression <: AnyFlashExpression
    val flashExpression: FlashExpression
  }
  abstract class MergedReads[DT <: AnyMergedReadsType, FE <: AnyFlashExpression](
    val dataType: DT,
    val flashExpression: FE,
    val label: String
  ) extends AnyMergedReads {

    type DataType = DT
    type FlashExpression = FE
  }

  abstract class MergedReadStats(val label: String) extends AnyData {

    type DataType = MergedReadStatsType.type
    val dataType = MergedReadStatsType
  }

}
