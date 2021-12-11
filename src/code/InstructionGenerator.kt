package code

import elements.identifier.VariableIdentifier
import elements.Program
import value_analysis.DynamicValue
import instructions.Instruction
import java.util.*

class InstructionGenerator {
	val linkedDynamicValues = HashMap<VariableIdentifier, DynamicValue>()
	val instructions = LinkedList<Instruction>()

	fun generateInstructions(program: Program): MutableList<Instruction> {
		program.generateInstructions(this)
		return instructions
	}
}