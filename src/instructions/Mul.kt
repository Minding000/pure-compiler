package instructions

import objects.Register

class Mul(outputRegister: Register, leftRegister: Register, rightRegister: Register, val isDivision: Boolean):
	BinaryInstruction(outputRegister, leftRegister, rightRegister)