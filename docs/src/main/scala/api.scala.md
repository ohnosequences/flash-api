
```scala
package ohnosequences.flash

import ohnosequences.cosas._, types._, properties._, records._, typeSets._
import ohnosequences.cosas.ops.typeSets.MapToList
import java.io.File
import shapeless.poly._

case object api {

  sealed trait AnyFlashCommand {

    lazy val name: String = toString

    type Arguments  <: AnyRecord { type PropertySet <: AnyPropertySet.withBound[AnyFlashOption] }
    type Options    <: AnyRecord { type PropertySet <: AnyPropertySet.withBound[AnyFlashOption] }
  }
  abstract class FlashCommand extends AnyFlashCommand

  trait AnyFlashOption extends AnyProperty {

    lazy val cmdName: String = toString replace("_", "-")
    // this is what is used for generating the Seq[String] cmd
    lazy val label: String = s"--${cmdName}"

    val valueToCmd: Raw => Seq[String]
  }
  abstract class FlashOption[V](val valueToCmd: V => Seq[String]) extends AnyFlashOption { type Raw = V }

  object AnyFlashOption {
    type is[FO <: AnyFlashOption] = FO with AnyFlashOption { type Raw = FO#Raw }
  }
```


### `Seq[String]` Command generation

for command values we generate a `Seq[String]` which is valid command expression that you can execute (assuming FLASh installed) using `scala.sys.process` or anything similar.


```scala
  trait DefaultOptionValueToSeq extends shapeless.Poly1 {

    implicit def default[FO <: AnyFlashOption](implicit option: AnyFlashOption.is[FO]) =
      at[ValueOf[FO]]{ v: ValueOf[FO] =>
        Seq(option.label) ++ option.valueToCmd(v.value).filterNot(_.isEmpty)
      }
  }
  case object optionValueToSeq extends DefaultOptionValueToSeq {

    implicit def atInput(implicit option: input.type) =
      at[ValueOf[input.type]]{ v: ValueOf[input.type] => option.valueToCmd(v.value) }

    implicit def atOutput(implicit out: output.type) =
      at[ValueOf[output.type]]{ v: ValueOf[output.type] => out.valueToCmd(v.value) }
  }
```


### Flash expressions

Flash expressions are valid commands that can be executed, using the typeclass provided `cmd: Seq[String]` method.


```scala
  trait AnyFlashExpression {

    type Command <: AnyFlashCommand
    val command: Command

    val optionValues: ValueOf[Command#Options]
    val argumentValues: ValueOf[Command#Arguments]
  }
  case class FlashExpression[FC <: AnyFlashCommand](
    val command: FC)(
    val argumentValues: ValueOf[FC#Arguments],
    val optionValues: ValueOf[FC#Options]
  )
  extends AnyFlashExpression {

    type Command = FC
  }

  implicit def flashExpressionOps[FE <: AnyFlashExpression](expr: FE): FlashExpressionOps[FE] =
    FlashExpressionOps(expr)
  case class FlashExpressionOps[FE <: AnyFlashExpression](val expr: FE) extends AnyVal {

    def cmd(implicit
      mapArgs: (optionValueToSeq.type MapToList FE#Command#Arguments#Raw) { type O = Seq[String] },
      mapOpts: (optionValueToSeq.type MapToList FE#Command#Options#Raw) { type O = Seq[String] }
    ): Seq[String] = {

      val (argsSeqs, optsSeqs): (List[Seq[String]], List[Seq[String]]) = (
        (expr.argumentValues.value: FE#Command#Arguments#Raw) mapToList optionValueToSeq,
        (expr.optionValues.value: FE#Command#Options#Raw)     mapToList optionValueToSeq
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
    case object arguments extends Record(input :&: output :&: □)

    type Options = options.type
    case object options extends Record(
      min_overlap            :&:
      max_overlap            :&:
      read_len              :&:
      fragment_len          :&:
      fragment_len_stddev   :&:
      threads               :&:
      allow_outies          :&:
      phred_offset          :&:
      cap_mismatch_quals    :&: □
    )

    lazy val defaults = options(
      min_overlap(10)            :~:
      max_overlap(65)            :~:
      read_len(100)             :~:
      fragment_len(180)         :~:
      fragment_len_stddev(18)   :~:
      threads(1)                :~:
      allow_outies(false)       :~:
      phred_offset(_33)         :~:
      cap_mismatch_quals(false) :~: ∅
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
    Seq("--output-directory", fout.outputPath.getCanonicalPath.toString)
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

    lazy val mergedReads            = new File(outputPath, s"${prefix}.extendedFrags.fastq")
    lazy val pair1NotMerged         = new File(outputPath, s"${prefix}.notCombined_1.fastq")
    lazy val pair2NotMerged         = new File(outputPath, s"${prefix}.notCombined_2.fastq")
    lazy val lengthNumericHistogram = new File(outputPath, s"${prefix}.hist")
    lazy val lengthVisualHistogram  = new File(outputPath, s"${prefix}.histogram")
  }

  // TODO add naive parsers and serializers
  val intParser: String => Option[Int] = str => {
      import scala.util.control.Exception._
      catching(classOf[NumberFormatException]) opt str.toInt
    }

  type mergedReadLength = mergedReadLength.type
  case object mergedReadLength extends Property[Int]("mergedReadLength")
  implicit val parseMergeReadLengthParser: PropertyParser[mergedReadLength,String] =
    PropertyParser(mergedReadLength, mergedReadLength.label){ intParser }
  implicit val parseMergeReadLengthSerializer: PropertySerializer[mergedReadLength,String] =
    PropertySerializer(mergedReadLength, mergedReadLength.label){ v => Some(v.toString) }

  type readNumber = readNumber.type
  case object readNumber       extends Property[Int]("readNumber")
  implicit val readNumberParser: PropertyParser[readNumber,String] =
    PropertyParser(readNumber, readNumber.label){ intParser }
  implicit val readNumberSerializer: PropertySerializer[readNumber,String] =
    PropertySerializer(readNumber, readNumber.label){ v => Some(v.toString) }

  case object mergedStats extends Record(mergedReadLength :&: readNumber :&: □)

  implicit def flashOutputOps[FO <: FlashOutput](output: FO): FlashOutputOps[FO] = FlashOutputOps(output)
  case class FlashOutputOps[FO <: FlashOutput](output: FO) extends AnyVal {

    import ops.typeSets.ParseDenotationsError
    // TODO better type (File errors etc)
    final def stats: Seq[Either[ParseDenotationsError,ValueOf[mergedStats.type]]] = {

      import com.github.tototoshi.csv._
      val csvReader = CSVReader.open(output.lengthNumericHistogram)(new TSVFormat {})

      def rows(lines: Iterator[Seq[String]])(headers: Seq[String]): Iterator[Map[String,String]] =
        lines map { line => (headers zip line) toMap }

      rows(csvReader iterator)(mergedStats.properties mapToList typeLabel) map { mergedStats parse _ } toList
    }
  }
```


We are restricting Flash input to be provided as a pair of `fastq` files, specified through a value of type `FlashInput`


```scala
  case object input extends FlashOption[FlashInput]( fin =>
    Seq(fin.pair1.getCanonicalPath.toString, fin.pair2.getCanonicalPath.toString)
  )
  sealed trait FlashInput {

    val pair1: File
    val pair2: File
  }
  case class FlashInputAt(val pair1: File, val pair2: File) extends FlashInput
}

```




[test/scala/CommandGeneration.scala]: ../../test/scala/CommandGeneration.scala.md
[test/scala/ParseMergeStats.scala]: ../../test/scala/ParseMergeStats.scala.md
[main/scala/api.scala]: api.scala.md
[main/scala/data.scala]: data.scala.md