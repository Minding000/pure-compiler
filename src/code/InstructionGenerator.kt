package code

import elements.Program
import objects.Element
import objects.Instruction
import objects.PlaceholderRegister
import objects.Register
import java.util.*

class InstructionGenerator {
	val voidRegister = PlaceholderRegister()
	val namedRegisters = HashMap<String, Register>()
	val instructions = LinkedList<Instruction>()
	var registerCount = 0

	fun generateInstructions(program: Program): MutableList<Instruction> {
		program.generateInstructions(this)
		return instructions
	}

	fun createRegister(): Register {
		return Register(registerCount++)
	}
}