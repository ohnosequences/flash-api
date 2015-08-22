package ohnosequences.flash

import ohnosequences.cosas._, types._, properties._, records._, typeSets._, ops.typeSets.{ CheckForAll, ToList }
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
    lazy val label: String = s"--${cmdName}"

    val valueToCmd: Raw => Seq[String]
  }
  abstract class FlashOption[V](val valueToCmd: V => Seq[String]) extends AnyFlashOption { type Raw = V }

  object AnyFlashOption {
    type is[FO <: AnyFlashOption] = FO with AnyFlashOption { type Raw = FO#Raw }
  }
  /*
    ### `Seq[String]` Command generation

    for command values we generate a `Seq[String]` which is valid command expression that you can execute (assuming FLASh installed) using `scala.sys.process` or anything similar.
  */
  trait DefaultOptionValueToSeq extends shapeless.Poly1 {

    implicit def default[FO <: AnyFlashOption](implicit option: AnyFlashOption.is[FO]) =
      at[ValueOf[FO]]{ v: ValueOf[FO] => ( Seq(option.label) ++ option.valueToCmd(v.value) ).filterNot(_.isEmpty) }
  }
  case object optionValueToSeq extends DefaultOptionValueToSeq {

    implicit def atInput(implicit option: input.type) =
      at[ValueOf[input.type]]{ v: ValueOf[input.type] => option.valueToCmd(v.value) }

    implicit def atOutput(implicit out: output.type) =
      at[ValueOf[output.type]]{ v: ValueOf[output.type] => out.valueToCmd(v.value) }
  }


  // case class FlashStatement[
  //   Cmd <: AnyFlashCommand,
  //   Opts <: AnyTypeSet.Of[AnyFlashOption]
  // ](
  //   val command: Cmd,
  //   val options: Opts
  // )(implicit
  //   val ev: CheckForAll[Opts, OptionFor[Cmd]],
  //   val toListEv: ToListOf[Opts, AnyFlashOption],
  //   val allArgs: Cmd#Arguments ⊂ Opts
  // )
  // {
  //   def toSeq: Seq[String] =  Seq(command.name) ++
  //                             ( (options.toListOf[AnyFlashOption]) flatMap { _.toSeq } )
  // }
  //
  // implicit def getFlashCommandOps[BC <: AnyFlashCommand](cmd: BC): FlashCommandOps[BC] =
  //   FlashCommandOps(cmd)
  //
  // case class FlashCommandOps[Cmd <: AnyFlashCommand](val cmd: Cmd) {
  //
  //   def withOptions[
  //     Opts <: AnyTypeSet.Of[AnyFlashOption]
  //   ](opts: Opts)(implicit
  //     ev: CheckForAll[Opts, OptionFor[Cmd]],
  //     toListEv: ToListOf[Opts, AnyFlashOption],
  //     allArgs: Cmd#Arguments ⊂ Opts
  //   ): FlashStatement[Cmd,Opts] = FlashStatement(cmd, opts)
  // }

  type flash = flash.type
  case object flash extends FlashCommand {

    type Arguments = arguments.type
    case object arguments extends Record(input :&: output :&: □)

    type Options = options.type
    case object options extends Record(minOverlap :&: maxOverlap :&: □)
  }

  case object minOverlap          extends FlashOption[Int]( x => Seq(x.toString) )
  case object maxOverlap          extends FlashOption[Int]( x => Seq(x.toString) )
  case object threads             extends FlashOption[Int]( x => Seq(x.toString) )
  case object readLen             extends FlashOption[Float]( x => Seq(x.toString) )
  case object fragment_len        extends FlashOption[Float]( x => Seq(x.toString) )
  case object fragment_len_stddev extends FlashOption[Float]( x => Seq(x.toString) )
  /*
    FLASh outputs **5** files:

    - `out.extendedFrags.fastq`      The merged reads.
    - `out.notCombined_1.fastq`      Read 1 of mate pairs that were not merged.
    - `out.notCombined_2.fastq`      Read 2 of mate pairs that were not merged.
    - `out.hist`                     Numeric histogram of merged read lengths.
    - `out.histogram`                Visual histogram of merged read lengths.

    The `out` prefix is configurable.
  */
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

  case object input extends FlashOption[FlashInput]( fin =>
    Seq(fin.pair1.getCanonicalPath.toString, fin.pair2.getCanonicalPath.toString)
  )
  sealed trait FlashInput {

    val pair1: File
    val pair2: File
  }
  case class FlashInputAt(val pair1: File, val pair2: File) extends FlashInput




}
