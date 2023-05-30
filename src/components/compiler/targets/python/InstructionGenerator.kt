package components.compiler.targets.python

import components.compiler.targets.python.instructions.Instruction
import components.syntax_parser.syntax_tree.general.Program
import java.util.*

class InstructionGenerator {

	fun generateInstructions(program: Program): MutableList<Instruction> {
		val instructions = LinkedList<Instruction>()
		//TODO generate instructions
		return instructions
	}
}
