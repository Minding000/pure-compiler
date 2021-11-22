package instructions

import errors.CompilerError
import objects.Instruction
import objects.Register
import objects.Value
import objects.ValueSource
import java.util.*

class Copy(targetRegister: Register, var sourceRegister: Register): WriteInstruction(targetRegister) {

	override fun getReadRegisters(): List<Register> {
		val list = LinkedList<Register>()
		list.add(sourceRegister)
		return list
	}

	override fun replace(current: Register, new: ValueSource) {
		if(new !is Register)
			throw CompilerError("Cannot use non-register in copy instruction.")
		if(targetRegister == current)
			targetRegister = new
		if(sourceRegister == current)
			sourceRegister = new
	}

	override fun getValueSource(): ValueSource {
		return sourceRegister
	}
}