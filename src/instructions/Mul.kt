package instructions

import objects.Instruction
import objects.Register

class Mul(val outputRegister: Register, val leftRegister: Register, val rightRegister: Register, val isDivision: Boolean): Instruction()