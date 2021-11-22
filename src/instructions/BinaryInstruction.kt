package instructions

import objects.Instruction
import objects.Register
import objects.ValueSource
import java.util.*

open class BinaryInstruction(var outputRegister: Register, var leftValueSource: ValueSource, var rightValueSource: ValueSource): Instruction() {

	override fun getWrittenRegisters(): List<Register> {
		val list = LinkedList<Register>()
		list.add(outputRegister)
		return list
	}

	override fun getReadRegisters(): List<Register> {
		val list = LinkedList<Register>()
		val left = leftValueSource
		if(left is Register)
			list.add(left)
		val right = rightValueSource
		if(right is Register)
			list.add(right)
		return list
	}

	override fun replace(current: Register, new: ValueSource) {
		if(new is Register && outputRegister == current)
			outputRegister = new
		if(leftValueSource == current)
			leftValueSource = new
		if(rightValueSource == current)
			rightValueSource = new
	}
}