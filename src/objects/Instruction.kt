package objects

import java.util.*

abstract class Instruction {

	fun getRegisters(): List<Register> {
		val list = LinkedList<Register>()
		list.addAll(getWrittenRegisters())
		list.addAll(getReadRegisters())
		return list
	}

	open fun getWrittenRegisters(): List<Register> {
		return LinkedList()
	}

	open fun getReadRegisters(): List<Register> {
		return LinkedList()
	}

	fun writesTo(register: Register): Boolean {
		return getWrittenRegisters().contains(register)
	}

	abstract fun replace(current: Register, new: ValueSource)
}