package instructions

import objects.Instruction
import objects.Register
import objects.ValueSource
import java.util.*

abstract class WriteInstruction(var targetRegister: Register): Instruction() {

	override fun getWrittenRegisters(): List<Register> {
		val list = LinkedList<Register>()
		list.add(targetRegister)
		return list
	}

	abstract fun getValueSource(): ValueSource
}