package elements

import code.InstructionGenerator
import source_structure.Position
import source_structure.Section
import value_analysis.ValueSource

abstract class Element(start: Position, end: Position): Section(start, end) {
	abstract fun generateInstructions(generator: InstructionGenerator): ValueSource?
}