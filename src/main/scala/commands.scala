package ohnosequences.flash.api

import ohnosequences.cosas._, types._, records._, klists._

sealed trait AnyFlashCommand {

  lazy val name: String = toString

  type Arguments <: AnyRecordType.withBound[AnyFlashOption]
  type Options   <: AnyRecordType.withBound[AnyFlashOption]

  type ArgumentsVals <: Arguments#Raw
  type OptionsVals   <: Options#Raw

  /* default values for options; they are *optional*, so should have default values. */
  val defaults: Options := OptionsVals
}

/*
  ### Flash command instance

  There is just one command part of the Flash suite, called `flash`.
*/
// type flash = flash.type
case object flash extends AnyFlashCommand {

  type Arguments = arguments.type
  case object arguments extends RecordType(
    input  :×:
    output :×:
    |[AnyFlashOption]
  )

  type ArgumentsVals =
    (input.type  := input.Raw)  ::
    (output.type := output.Raw) ::
    *[AnyDenotation]

  type Options = options.type
  case object options extends RecordType(
    min_overlap         :×:
    max_overlap         :×:
    threads             :×:
    allow_outies        :×:
    phred_offset        :×:
    cap_mismatch_quals  :×:
    |[AnyFlashOption]
  )

  type OptionsVals =
    (min_overlap.type         := min_overlap.Raw)         ::
    (max_overlap.type         := max_overlap.Raw)         ::
    (threads.type             := threads.Raw)             ::
    (allow_outies.type        := allow_outies.Raw)        ::
    (phred_offset.type        := phred_offset.Raw)        ::
    (cap_mismatch_quals.type  := cap_mismatch_quals.Raw)  ::
    *[AnyDenotation]

  val defaults: Options := OptionsVals = options(
    min_overlap(10)           ::
    max_overlap(65)           ::
    threads(1)                ::
    allow_outies(false)       ::
    phred_offset(_33)         ::
    cap_mismatch_quals(false) ::
    *[AnyDenotation]
  )

  def apply(
    argumentValues: ArgumentsVals,
    optionValues: OptionsVals
  ): FlashExpression[this.type] =
    FlashExpression(this)(
      argumentValues,
      optionValues
    )
}
