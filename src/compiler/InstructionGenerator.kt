package compiler

import components.syntax_parser.syntax_tree.general.Program
import compiler.instructions.Instruction
import java.util.*

class InstructionGenerator {

	fun generateInstructions(program: Program): MutableList<Instruction> {
		val instructions = LinkedList<Instruction>()
		//TODO generate instructions
		return instructions
	}
}
