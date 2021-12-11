package elements.operations

import code.InstructionGenerator
import instructions.Add
import value_analysis.DynamicValue
import elements.ValueElement

class Addition(val left: ValueElement, val right: ValueElement, val isSubtraction: Boolean): ValueElement(left.start, right.end, left.type) {

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
		val addInstruction = Add(
			left.generateInstructions(generator),
			right.generateInstructions(generator),
			isSubtraction
		)
		generator.instructions.add(addInstruction)
		return addInstruction.output
	}

	override fun toString(): String {
		return "Addition { $left ${ if(isSubtraction) "-" else "+" } $right }"
	}
}