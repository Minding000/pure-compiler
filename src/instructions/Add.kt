package instructions

import objects.Instruction
import objects.Register

class Add(val outputRegister: Register, val leftRegister: Register, val rightRegister: Register, val isNegative: Boolean): Instruction()