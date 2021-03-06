
```scala
package ohnosequences.flash

import ohnosequences.cosas._, types._, records._, fns._, klists._
import scala.collection.JavaConversions._
import java.nio.file.Files
import java.io._

case object api {

  sealed trait AnyFlashCommand {

    lazy val name: String = toString

    type Arguments <: AnyRecordType { type Keys <: AnyProductType { type Types <: AnyKList { type Bound <: AnyFlashOption } } }
    type Options   <: AnyRecordType { type Keys <: AnyProductType { type Types <: AnyKList { type Bound <: AnyFlashOption } } }
  }
  abstract class FlashCommand extends AnyFlashCommand

  trait AnyFlashOption extends AnyType {

    lazy val cmdName: String = toString replace("_", "-")
    // this is what is used for generating the Seq[String] cmd
    lazy val label: String = s"--${cmdName}"

    val valueToCmd: Raw => Seq[String]
  }
  abstract class FlashOption[V](val valueToCmd: V => Seq[String]) extends AnyFlashOption { type Raw = V }

  case object AnyFlashOption {
    type is[FO <: AnyFlashOption] = FO with AnyFlashOption { type Raw = FO#Raw }
  }
```


### `Seq[String]` Command generation

for command values we generate a `Seq[String]` which is valid command expression that you can execute (assuming FLASh installed) using `scala.sys.process` or anything similar.


```scala
  trait DefaultOptionValueToSeq extends DepFn1[Any, Seq[String]] {

    implicit def default[FO <: AnyFlashOption, V <: FO#Raw](implicit
      option: FO with AnyFlashOption { type Raw = FO#Raw }
    )
    : AnyApp1At[optionValueToSeq.type, FO := V] { type Y = Seq[String] }=
      App1 { v: FO := V => Seq(option.label) ++ option.valueToCmd(v.value).filterNot(_.isEmpty) }
  }
  case object optionValueToSeq extends DefaultOptionValueToSeq {

    implicit def atInput[V <: input.Raw]: AnyApp1At[optionValueToSeq.type, input.type := V] { type Y  = Seq[String] } =
      App1 { v: input.type := V => input.valueToCmd(v.value) }

    implicit def atOutput[V <: output.Raw]: AnyApp1At[optionValueToSeq.type, output.type := V]  { type Y = Seq[String] } =
      App1 { v: output.type := V => output.valueToCmd(v.value) }
  }
```


### Flash expressions

Flash expressions are valid commands that can be executed, using the typeclass provided `cmd: Seq[String]` method.


```scala
  trait AnyFlashExpression {

    type Command = flash
    val command: Command = flash

    type ValArgs <: flash.Arguments#Raw
    type ValOpt <: flash.Options#Raw

    val argumentValues: flash.Arguments := ValArgs
    val optionValues: flash.Options := ValOpt
  }
  case class FlashExpression[
    AV <: flash.Arguments#Raw,
    OV <: flash.Options#Raw
  ](
    val argumentValues: flash.Arguments := AV,
    val optionValues: flash.Options := OV
  )
  extends AnyFlashExpression {

    type ValArgs = AV; type ValOpt = OV
  }

  implicit def flashExpressionOps[FE <: AnyFlashExpression](expr: FE): FlashExpressionOps[FE] =
    FlashExpressionOps(expr)
  case class FlashExpressionOps[FE <: AnyFlashExpression](val expr: FE) extends AnyVal {

    def cmd[
      AO <: AnyKList.withBound[Seq[String]],
      OO <: AnyKList.withBound[Seq[String]]
    ](implicit
      mapArgs: AnyApp2At[mapKList[optionValueToSeq.type, Seq[String]], optionValueToSeq.type, FE#ValArgs] { type Y = AO },
      mapOpts: AnyApp2At[mapKList[optionValueToSeq.type, Seq[String]], optionValueToSeq.type, FE#ValOpt] { type Y = OO }
    )
    : Seq[String] = {

      val (argsSeqs, optsSeqs): (List[Seq[String]], List[Seq[String]]) = (
        (expr.argumentValues.value: FE#ValArgs) map optionValueToSeq asList,
        (expr.optionValues.value: FE#ValOpt) map optionValueToSeq asList
      )

      Seq(expr.command.name) ++ argsSeqs.toSeq.flatten ++ optsSeqs.toSeq.flatten
    }
  }
```


### Flash command instances

There is just one command part of the Flash suite, called `flash`.


```scala
  type flash = flash.type
  case object flash extends FlashCommand {

    type Arguments = arguments.type
    case object arguments extends RecordType(input :×: output :×: |[AnyFlashOption])

    type Options = options.type
    case object options extends RecordType(
      min_overlap            :×:
      max_overlap            :×:
      read_len              :×:
      fragment_len          :×:
      fragment_len_stddev   :×:
      threads               :×:
      allow_outies          :×:
      phred_offset          :×:
      cap_mismatch_quals    :×: |[AnyFlashOption]
    )

    lazy val defaults = options(
      min_overlap(10)           ::
      max_overlap(65)           ::
      read_len(100)             ::
      fragment_len(180)         ::
      fragment_len_stddev(18)   ::
      threads(1)                ::
      allow_outies(false)       ::
      phred_offset(_33)         ::
      cap_mismatch_quals(false) :: *[AnyDenotation]
    )
  }
```


### Flash options

Instances for all `flash` options available.


```scala
  case object min_overlap          extends FlashOption[Int]( x => Seq(x.toString) )
  case object max_overlap          extends FlashOption[Int]( x => Seq(x.toString) )
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
```


### Flash input and output

FLASh outputs **5** files:

- `out.extendedFrags.fastq`      The merged reads.
- `out.notCombined_1.fastq`      Read 1 of mate pairs that were not merged.
- `out.notCombined_2.fastq`      Read 2 of mate pairs that were not merged.
- `out.hist`                     Numeric histogram of merged read lengths.
- `out.histogram`                Visual histogram of merged read lengths.

The `out` prefix is configurable. For that you just provide an instance of type `FlashOutput`.


```scala
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

    lazy val mergedReads: File            = new File(s"${outputPath}/${prefix}.extendedFrags.fastq")
    lazy val pair1NotMerged: File         = new File(s"${outputPath}/${prefix}.notCombined_1.fastq")
    lazy val pair2NotMerged: File         = new File(s"${outputPath}/${prefix}.notCombined_2.fastq")
    lazy val lengthNumericHistogram: File = new File(s"${outputPath}/${prefix}.hist")
    lazy val lengthVisualHistogram: File  = new File(s"${outputPath}/${prefix}.histogram")
  }

  // TODO add naive parsers and serializers
  val intParser: String => Option[Int] = str => {
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

  implicit def flashOutputOps[FO <: FlashOutput](output: FO): FlashOutputOps[FO] = FlashOutputOps(output)
  case class FlashOutputOps[FO <: FlashOutput](output: FO) extends AnyVal {

    // TODO better type (File errors etc)
    final def stats: Iterator[
      Either[
        ParseDenotationsError,
        mergedStats.type := ( (mergedReadLength.type := Int) :: (readNumber := Int) :: *[AnyDenotation] )
      ]
    ] = {

      val header: Seq[String] = mergedStats.keys.types map typeLabel toList

      Files
        .lines(output.lengthNumericHistogram.toPath).iterator
        .map { str: String =>

          val row: Seq[String] = str.split('\t')
          mergedStats parse (header zip row).toMap
        }
    }
  }
```


We are restricting Flash input to be provided as a pair of `fastq` files, specified through a value of type `FlashInput`


```scala
  case object input extends FlashOption[FlashInput]( fin =>
    Seq(fin.pair1.getCanonicalPath, fin.pair2.getCanonicalPath)
  )
  sealed trait FlashInput {

    val pair1: File
    val pair2: File
  }
  case class FlashInputAt(val pair1: File, val pair2: File) extends FlashInput
}

```




[main/scala/api.scala]: api.scala.md
[test/scala/CommandGeneration.scala]: ../../test/scala/CommandGeneration.scala.md
[test/scala/ParseMergeStats.scala]: ../../test/scala/ParseMergeStats.scala.md