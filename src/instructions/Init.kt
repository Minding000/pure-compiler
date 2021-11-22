package instructions

import errors.CompilerError
import objects.Instruction
import objects.Register
import objects.Value
import objects.ValueSource
import java.util.*

class Init(targetRegister: Register, val value: Value): WriteInstruction(targetRegister) {

	override fun replace(current: Register, new: ValueSource) {
		if(new !is Register)
			throw CompilerError("Cannot assign to non-register in init instruction.")
		if(targetRegister == current)
			targetRegister = new
	}

	override fun getValueSource(): ValueSource {
		return value
	}
}