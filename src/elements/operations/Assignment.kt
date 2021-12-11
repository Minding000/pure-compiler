package elements.operations

import code.InstructionGenerator
import elements.identifier.VariableIdentifier
import instructions.Copy
import value_analysis.DynamicValue
import elements.ValueElement
import elements.VoidElement
import elements.identifier.Variable

class Assignment(val identifier: Variable, val value: ValueElement): VoidElement(identifier.start, value.end) {

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue? {
		// Mind the execution order
		val valueSource = value.generateInstructions(generator)
		generator.instructions.add(Copy(
			identifier.getNewDynamicValue(generator),
			valueSource
		))
		return null
	}

	override fun toString(): String {
		return "Assignment { $identifier = $value }"
	}
}