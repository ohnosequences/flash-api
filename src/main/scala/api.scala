package ohnosequences.flash

import ohnosequences.cosas._, properties._, records._, typeSets._, ops.typeSets.{ CheckForAll, ToList }
import java.io.File

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
  }
  abstract class FlashOption[V] extends AnyFlashOption { type Raw = V }

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
    case object arguments extends Record(□)

    type Options = options.type
    case object options extends Record(minOverlap :&: maxOverlap :&: □)


  }

  case object minOverlap          extends FlashOption[Int]
  case object maxOverlap          extends FlashOption[Int]
  case object threads             extends FlashOption[Int]
  case object readLen             extends FlashOption[Float]
  case object fragment_len        extends FlashOption[Float]
  case object fragment_len_stddev extends FlashOption[Float]
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
  case object output              extends FlashOption[FlashOutput]
  sealed trait FlashOutput {

    val mergedReads: File
    val pair1NotMerged: File
    val pair2NotMerged: File
    val lengthNumericHistogram: File
    val lengthVisualHistogram: File
  }
  case class FlashOutputAt(val outputPath: File, val prefix: String) extends FlashOutput {

    lazy val mergedReads = new File(outputPath, s"${prefix}.extendedFrags.fastq")
    lazy val pair1NotMerged = new File(outputPath, s"${prefix}.notCombined_1.fastq")
    lazy val pair2NotMerged = new File(outputPath, s"${prefix}.notCombined_2.fastq")
    lazy val lengthNumericHistogram = new File(outputPath, s"${prefix}.hist")
    lazy val lengthVisualHistogram = new File(outputPath, s"${prefix}.histogram")
  }

  case object input               extends FlashOption[FlashInput]
  sealed trait FlashInput {

    val pair1: File
    val pair2: File
  }
  case class FlashInputAt(val pair1: File, val pair2: File) extends FlashInput
}
