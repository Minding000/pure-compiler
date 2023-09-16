package components.code_generation.python.value_analysis

import components.code_generation.python.instructions.Instruction
import errors.internal.CompilerError
import java.util.*

class DynamicValue: ValueSource {
	private var _writeInstruction: Instruction? = null
	val usages = LinkedList<Instruction>()
	var register: Register? = null

	fun setWriteInstruction(instruction: Instruction) {
		_writeInstruction = instruction
	}

	fun getWriteInstruction(): Instruction {
		return _writeInstruction ?: throw CompilerError("Accessing uninitialized dynamic value.")
	}

	override fun toString(): String {
		return register?.toString() ?: "dv${super.hashCode() % 1000}"
	}
}
