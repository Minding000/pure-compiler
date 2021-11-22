package instructions

import errors.CompilerError
import objects.Instruction
import objects.Register
import objects.ValueSource

class Eql: Instruction() {

	override fun replace(current: Register, new: ValueSource) {
		throw CompilerError("Equals is not yet implemented.")
	}
}