package elements.identifier

import code.InstructionGenerator
import source_structure.Position
import value_analysis.DynamicValue

interface Variable {
	val start: Position
	val end: Position

	fun getNewDynamicValue(instructionGenerator: InstructionGenerator): DynamicValue
}