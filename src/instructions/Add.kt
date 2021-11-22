package instructions

import objects.Register

class Add(outputRegister: Register, leftRegister: Register, rightRegister: Register, val isNegative: Boolean):
	BinaryInstruction(outputRegister, leftRegister, rightRegister) {
}