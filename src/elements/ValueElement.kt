package elements

import code.InstructionGenerator
import errors.user.ResolveError
import source_structure.Position
import types.Type
import value_analysis.ValueSource

abstract class ValueElement(start: Position, end: Position, var type: Type? = null) : Element(start, end) {

	fun requireType(): Type {
		return type ?: throw ResolveError("Value element does not have a type.")
	}

	abstract override fun generateInstructions(generator: InstructionGenerator): ValueSource

	override fun toString(): String {
		return "ValueElement { --- }"
	}
}