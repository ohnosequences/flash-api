package ohnosequences.flash.api

/*
  ### Flash expressions

  Flash expressions are valid commands that can be executed, using the typeclass provided `toSeq: Seq[String]` method.
*/
trait AnyFlashExpression {

  type Command <: AnyFlashCommand
  val  command: Command

  val argumentValues: Command#ArgumentsVals
  val optionValues:   Command#OptionsVals

  // implicitly:
  val argValsToSeq: FlashOptionsToSeq[Command#ArgumentsVals]
  val optValsToSeq: FlashOptionsToSeq[Command#OptionsVals]

  /* For command values we generate a `Seq[String]` which is valid command expression that you can
     execute (assuming BLAST installed) using `scala.sys.process` or anything similar. */
  def toSeq: Seq[String] =
    Seq(command.name) ++
    argValsToSeq(argumentValues) ++
    optValsToSeq(optionValues)
}

case class FlashExpression[
  C <: AnyFlashCommand
](val command: C
)(val argumentValues: C#ArgumentsVals,
  val optionValues:   C#OptionsVals
)(implicit
  val argValsToSeq: FlashOptionsToSeq[C#ArgumentsVals],
  val optValsToSeq: FlashOptionsToSeq[C#OptionsVals]
) extends AnyFlashExpression {

  type Command = C
}
