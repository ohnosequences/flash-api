package ohnosequences.flash.api

import ohnosequences.cosas._, types._, records._, fns._, klists._
import scala.collection.JavaConverters._
import java.nio.file.Files
import java.io._

trait AnyFlashOption extends AnyType {

  lazy val cmdName: String = toString replace("_", "-")
  // this is what is used for generating the Seq[String] cmd
  lazy val label: String = s"--${cmdName}"

  val valueToCmd: Raw => Seq[String]
}

abstract class FlashOption[V](
  val valueToCmd: V => Seq[String]
) extends AnyFlashOption {
  type Raw = V
}

case object AnyFlashOption {
  type is[FO <: AnyFlashOption] = FO with AnyFlashOption { type Raw = FO#Raw }
}

/*
  ### `Seq[String]` Command generation

  for command values we generate a `Seq[String]` which is valid command expression that you can execute (assuming FLASh installed) using `scala.sys.process` or anything similar.
*/
trait DefaultOptionValueToSeq extends DepFn1[AnyDenotation, Seq[String]] {

  implicit def default[
    V,
    FO <: AnyFlashOption { type Raw = V }
  ]: AnyApp1At[
    optionValueToSeq.type,
    FO := V
  ] {
    type Y = Seq[String]
  } = App1 { v =>
    Seq(v.tpe.label) ++ v.tpe.valueToCmd(v.value).filterNot(_.isEmpty)
  }
}

case object optionValueToSeq extends DefaultOptionValueToSeq {

  implicit def atInput[
    V <: input.Raw
  ]: AnyApp1At[
    optionValueToSeq.type,
    input.type := V
  ] {
    type Y = Seq[String]
  } = App1 { v: input.type := V =>
    input.valueToCmd(v.value)
  }

  implicit def atOutput[
    V <: output.Raw
  ]: AnyApp1At[
    optionValueToSeq.type,
    output.type := V
  ] {
    type Y = Seq[String]
  } = App1 { v: output.type := V =>
    output.valueToCmd(v.value)
  }
}

/* This works as a type class, which provides a way of serializing a list of AnyFlashOption's */
trait FlashOptionsToSeq[L <: AnyKList.withBound[AnyDenotation]] {

  def apply(l: L): Seq[String]
}

case object FlashOptionsToSeq {

  implicit def default[
    L <: AnyKList.withBound[AnyDenotation],
    O <: AnyKList.withBound[Seq[String]]
  ](implicit
    mapp: AnyApp2At[
      mapKList[optionValueToSeq.type, Seq[String]],
      optionValueToSeq.type,
      L
    ] { type Y = O }
  ): FlashOptionsToSeq[L] = new FlashOptionsToSeq[L] {

    def apply(l: L): Seq[String] =
      mapp(optionValueToSeq, l).asList.flatten
  }
}

/*
  ### Flash options

  Instances for all `flash` options available.
*/
case object min_overlap         extends FlashOption[Int]( x => Seq(x.toString) )
case object max_overlap         extends FlashOption[Int]( x => Seq(x.toString) )
case object threads             extends FlashOption[Int]( x => Seq(x.toString) )
case object read_len            extends FlashOption[Int]( x => Seq(x.toString) )
case object fragment_len        extends FlashOption[Int]( x => Seq(x.toString) )
case object fragment_len_stddev extends FlashOption[Int]( x => Seq(x.toString) )
case object allow_outies        extends FlashOption[Boolean]( x => Seq() )
case object phred_offset        extends FlashOption[PhredOffset]( x => Seq(x.asciiValue.toString) )
sealed abstract class PhredOffset(val asciiValue: Int)
case object _33 extends PhredOffset(33)
case object _64 extends PhredOffset(64)
case object cap_mismatch_quals  extends FlashOption[Boolean]( x => Seq() )

/*
  ### Flash input and output

  FLASh outputs **5** files:

  - `out.extendedFrags.fastq`      The merged reads.
  - `out.notCombined_1.fastq`      Read 1 of mate pairs that were not merged.
  - `out.notCombined_2.fastq`      Read 2 of mate pairs that were not merged.
  - `out.hist`                     Numeric histogram of merged read lengths.
  - `out.histogram`                Visual histogram of merged read lengths.

  The `out` prefix is configurable. For that you just provide an instance of type `FlashOutput`.
*/
// this does not correspond directly to a FLASh option, but to a set of them
case object output extends FlashOption[FlashOutput]( fout =>
  Seq("--output-prefix", fout.prefix) ++
  Seq("--output-directory", fout.outputPath.getCanonicalPath)
)
sealed trait FlashOutput {

  val prefix: String
  val outputPath: File

  val mergedReads: File
  val pair1NotMerged: File
  val pair2NotMerged: File
  val lengthNumericHistogram: File
  val lengthVisualHistogram: File
}
case class FlashOutputAt(val outputPath: File, val prefix: String) extends FlashOutput {

  lazy val mergedReads: File            = new File(outputPath, s"${prefix}.extendedFrags.fastq")
  lazy val pair1NotMerged: File         = new File(outputPath, s"${prefix}.notCombined_1.fastq")
  lazy val pair2NotMerged: File         = new File(outputPath, s"${prefix}.notCombined_2.fastq")
  lazy val lengthNumericHistogram: File = new File(outputPath, s"${prefix}.hist")
  lazy val lengthVisualHistogram: File  = new File(outputPath, s"${prefix}.histogram")
}

case object FlashOutput {

  // TODO add naive parsers and serializers
  val intParser: String => Option[Int] = { str =>
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toInt
  }

  type mergedReadLength = mergedReadLength.type
  case object mergedReadLength extends Type[Int]("mergedReadLength")
  implicit val parseMergeReadLengthParser: DenotationParser[mergedReadLength,Int,String] =
    new DenotationParser(mergedReadLength, mergedReadLength.label)(intParser)
  implicit val parseMergeReadLengthSerializer: DenotationSerializer[mergedReadLength,Int,String] =
    new DenotationSerializer(mergedReadLength, mergedReadLength.label)({ v => Some(v.toString) })

  type readNumber = readNumber.type
  case object readNumber       extends Type[Int]("readNumber")
  implicit val readNumberParser: DenotationParser[readNumber,Int,String] =
    new DenotationParser(readNumber, readNumber.label)(intParser)
  implicit val readNumberSerializer: DenotationSerializer[readNumber,Int,String] =
    new DenotationSerializer(readNumber, readNumber.label)({ v => Some(v.toString) })

  case object mergedStats extends RecordType(mergedReadLength :×: readNumber :×: |[AnyType])

  implicit class FlashOutputOps[FO <: FlashOutput](val output: FO) extends AnyVal {

    // TODO better type (File errors etc)
    final def stats: Iterator[
      Either[
        ParseDenotationsError,
        mergedStats.type := ( (mergedReadLength.type := Int) :: (readNumber := Int) :: *[AnyDenotation] )
      ]
    ] = {

      val header: Seq[String] = mergedStats.keys.types map typeLabel toList

      Files
        .lines(output.lengthNumericHistogram.toPath)
        .iterator.asScala
        .map { str: String =>

          val row: Seq[String] = str.split('\t')
          mergedStats parse (header zip row).toMap
        }
    }
  }
}

/*
  We are restricting Flash input to be provided as a pair of `fastq` files, specified through a value of type `FlashInput`
*/
case object input extends FlashOption[FlashInput]( fin =>
  Seq(
    fin.pair1.getCanonicalPath,
    fin.pair2.getCanonicalPath
  )
)

sealed trait FlashInput {
  val pair1: File
  val pair2: File
}

case class FlashInputAt(
  val pair1: File,
  val pair2: File
) extends FlashInput
