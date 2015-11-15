package ohnosequences.flash

import ohnosequences.cosas._, types._
import ohnosequences.datasets._, illumina._
import api._

case object data {

  // data types
  case object MergedReadsStatsType extends AnyDataType {

    type Record = mergedStats.type
    val record: Record = mergedStats

    val label = toString
  }

  // In a future world this could link to the illumina read type
  trait AnyMergedReadsType extends AnyDataType {

    type ReadsType <: AnyReadsType { type EndType = pairedEndType }
    val readsType: ReadsType

    lazy val label = s"mergedReads.${readsType.label}"
  }
  class MergedReadsType[RT <: AnyReadsType { type EndType = pairedEndType }](val readsType: RT)
  extends AnyMergedReadsType {

    type ReadsType = RT
  }

  // data
  trait AnyMergedReads extends AnyData { mergedReads =>

    type ReadsType <: AnyReadsType { type EndType = pairedEndType }
    val readsType: ReadsType

    type DataType = MergedReadsType[ReadsType]
    lazy val dataType =  new MergedReadsType(readsType)

    type Reads1 <: reads.AnyPairedEnd1Fastq { type DataType = mergedReads.ReadsType }
    val reads1: Reads1

    type Reads2 <: reads.AnyPairedEnd2Fastq { type DataType = mergedReads.ReadsType }
    val reads2: Reads2

    val optionValues: flash#Options := flash#Options#Raw

    lazy val label = s"mergedReads.${reads1.label}.${reads2.label}"
  }
  class MergedReads[
    RT <: AnyReadsType { type EndType = pairedEndType },
    R1 <: reads.AnyPairedEnd1Fastq { type DataType = RT },
    R2 <: reads.AnyPairedEnd2Fastq { type DataType = RT }
  ]
  (
    val readsType: RT,
    val reads1: R1,
    val reads2: R2,
    val optionValues: flash#Options := flash#Options#Raw
  )
  extends AnyMergedReads {

    type ReadsType = RT

    type Reads1 = R1
    type Reads2 = R2
  }

  trait AnyMergedReadsStats extends AnyData {

    type DataType = MergedReadsStatsType.type
    val dataType = MergedReadsStatsType

    type MergedReads <: AnyMergedReads
    val mergedReads: MergedReads

    lazy val label = s"stats.${mergedReads.label}"
  }
  class MergedReadsStats[MR <: AnyMergedReads](val mergedReads: MR) extends AnyMergedReadsStats {

    type MergedReads = MR
  }

}
