package elements

import code.InstructionGenerator
import source_structure.Position
import value_analysis.ValueSource

abstract class VoidElement(start: Position, end: Position): Element(start, end) {

	override fun generateInstructions(generator: InstructionGenerator): ValueSource? {
		return null
	}

	override fun toString(): String {
		return "VoidElement { --- }"
	}
}